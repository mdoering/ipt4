/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.ipt.persistence;

import org.gbif.dwc.terms.Term;
import org.gbif.ipt.model.DataPackageField;
import org.gbif.ipt.model.DataPackageFieldMapping;
import org.gbif.ipt.model.DataPackageMapping;
import org.gbif.ipt.model.DataPackageSchema;
import org.gbif.ipt.model.DataPackageTableSchema;
import org.gbif.ipt.model.Extension;
import org.gbif.ipt.model.ExtensionMapping;
import org.gbif.ipt.model.ExtensionProperty;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.PropertyMapping;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.Source;
import org.gbif.ipt.model.User;
import org.gbif.ipt.model.VersionHistory;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replaces the stand-ins {@link DataDirJson} reads for references with the managed objects, the way IPT 3's XStream
 * converters resolved them. Unknown references become null (and a mapping keeps the row type it referred to in
 * {@link ExtensionMapping#getExtensionVerbatim()}); {@code ResourceLoader} then reports them as IPT 3 did.
 */
public class ResourceLinker {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceLinker.class);

  private final ReferenceLookup lookup;

  public ResourceLinker(ReferenceLookup lookup) {
    this.lookup = lookup;
  }

  public void link(Resource resource) {
    for (Source source : resource.getSources()) {
      source.setResource(resource);
    }
    resource.setCreator(user(resource.getCreator()));
    resource.setModifier(user(resource.getModifier()));
    Set<User> managers = new HashSet<>();
    for (User m : resource.getManagers()) {
      managers.add(user(m));
    }
    resource.setManagers(managers);
    for (VersionHistory vh : resource.getVersionHistory()) {
      vh.setModifiedBy(user(vh.getModifiedBy()));
    }
    Organisation org = resource.getOrganisation();
    if (org != null) {
      resource.setOrganisation(org.getKey() == null ? null : lookup.organisation(org.getKey().toString()));
    }

    for (ExtensionMapping mapping : resource.getMappings()) {
      mapping.setSource(source(resource, mapping.getSource()));
      Extension stub = mapping.getExtension();
      if (stub != null) {
        Extension extension = lookup.extension(stub.getRowType());
        if (extension == null) {
          LOG.warn("Resource {} maps to extension {}, which is not installed", resource.getShortname(), stub.getRowType());
        }
        mapping.setExtension(extension);
        mapping.setExtensionVerbatim(extension == null ? stub.getRowType() : extension.toString());
        // terms decide the order of the fields' TreeSet, so the set is rebuilt after replacing them
        Set<PropertyMapping> fields = new TreeSet<>();
        for (PropertyMapping pm : mapping.getFields()) {
          pm.setTerm(term(extension, pm.getTerm()));
          fields.add(pm);
        }
        mapping.setFields(fields);
      }
    }

    for (DataPackageMapping mapping : resource.getAllDataPackageMappings()) {
      mapping.setSource(source(resource, mapping.getSource()));
      DataPackageSchema stub = mapping.getDataPackageSchema();
      DataPackageSchema schema = stub == null ? null : lookup.dataPackageSchema(stub.getIdentifier());
      if (stub != null && schema == null) {
        LOG.warn("Resource {} maps to data package schema {}, which is not installed", resource.getShortname(),
            stub.getIdentifier());
      }
      mapping.setDataPackageSchema(schema);
      DataPackageTableSchema table = mapping.getTableSchema();
      for (DataPackageFieldMapping fm : mapping.getFields()) {
        fm.setField(field(table, fm.getField()));
      }
    }
  }

  @Nullable
  private User user(@Nullable User stub) {
    return stub == null || stub.getEmail() == null ? null : lookup.user(stub.getEmail());
  }

  @Nullable
  private static Source source(Resource resource, @Nullable Source stub) {
    if (stub == null) {
      return null;
    }
    Source source = resource.getSource(stub.getName());
    if (source == null) {
      LOG.warn("Resource {} maps source {}, which does not exist", resource.getShortname(), stub.getName());
    }
    return source;
  }

  private static Term term(@Nullable Extension extension, @Nullable Term stub) {
    // a key-only stand-in (e.g. while migrating) has no properties to resolve against
    if (stub == null || extension == null || extension.getProperties() == null || extension.getProperties().isEmpty()) {
      return stub;
    }
    ExtensionProperty property = extension.getProperty(stub.qualifiedName());
    if (property == null) {
      LOG.warn("Cant unmarshall concept {}", stub.qualifiedName());
      return stub;
    }
    return property;
  }

  @Nullable
  private static DataPackageField field(@Nullable DataPackageTableSchema table, @Nullable DataPackageField stub) {
    if (stub == null) {
      return null;
    }
    if (table != null) {
      for (DataPackageField field : table.getFields()) {
        if (field.getName().equals(stub.getName())) {
          return field;
        }
      }
    }
    LOG.warn("Cant unmarshall field {}", stub.getName());
    return null;
  }
}
