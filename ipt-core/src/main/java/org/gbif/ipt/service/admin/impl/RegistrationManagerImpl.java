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
package org.gbif.ipt.service.admin.impl;

import org.gbif.doi.service.DoiService;
import org.gbif.doi.service.datacite.RestJsonApiDataCiteService;
import org.gbif.ipt.config.AppConfig;
import org.gbif.ipt.config.DataDir;
import org.gbif.ipt.model.Ipt;
import org.gbif.ipt.model.Network;
import org.gbif.ipt.model.Organisation;
import org.gbif.ipt.model.Registration;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.persistence.RegistrationStore;
import org.gbif.ipt.persistence.SecretCipher;
import org.gbif.ipt.model.voc.DOIRegistrationAgency;
import org.gbif.ipt.service.AlreadyExistingException;
import org.gbif.ipt.service.BaseManager;
import org.gbif.ipt.service.DeletionNotAllowedException;
import org.gbif.ipt.service.DeletionNotAllowedException.Reason;
import org.gbif.ipt.service.InvalidConfigException;
import org.gbif.ipt.service.InvalidConfigException.TYPE;
import org.gbif.ipt.service.admin.RegistrationManager;
import org.gbif.ipt.service.registry.RegistryManager;
import org.gbif.ipt.utils.IptFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class RegistrationManagerImpl extends BaseManager implements RegistrationManager {

  private static final Logger LOG = LogManager.getLogger(RegistrationManagerImpl.class);

  private static final Comparator<Organisation> ORG_BY_NAME_ORD = (left, right) -> StringUtils.compare(left.getName(), right.getName());

  public static final String PERSISTENCE_FILE = RegistrationStore.FILENAME;
  private Registration registration = new Registration();
  private final RegistrationStore store;
  private RegistryManager registryManager;

  public RegistrationManagerImpl(
      AppConfig cfg,
      DataDir dataDir,
      RegistryManager registryManager,
      SecretCipher cipher) {
    super(cfg, dataDir);
    this.store = new RegistrationStore(dataDir.configFile(PERSISTENCE_FILE).toPath(), cipher);
    this.registryManager = registryManager;
  }

  @Override
  public Organisation addAssociatedOrganisation(Organisation organisation)
    throws AlreadyExistingException, InvalidConfigException {
    if (organisation != null) {

      // ensure max 1 DOI account is activated in the IPT
      if (organisation.isAgencyAccountPrimary() && findPrimaryDoiAgencyAccount() != null && !organisation.getKey()
        .equals(findPrimaryDoiAgencyAccount().getKey())) {
        throw new InvalidConfigException(TYPE.REGISTRATION_BAD_CONFIG,
          "Multiple DOI accounts activated in registration information - only one is allowed.");
      }

      LOG.debug("Adding/updating associated organisation {} - {}", organisation.getKey(), organisation.getName());
      registration.getAssociatedOrganisations().put(organisation.getKey().toString(), organisation);
    }
    return organisation;
  }

  /**
   * Find the organisation associated to the IPT that has a DOI agency account that has been activated. This
   * organisation's DOI agency account has been chosen as the only account used to register DOIs for
   * datasets.
   *
   * @return organisation with activated DOI agency account if found, null otherwise
   */
  @Override
  public Organisation findPrimaryDoiAgencyAccount() {
    for (Organisation organisation : registration.getAssociatedOrganisations().values()) {
      if (organisation.isAgencyAccountPrimary()) {
        return organisation;
      }
    }
    return null;
  }

  @Override
  public DoiService getDoiService() throws InvalidConfigException {
    Organisation organisation = findPrimaryDoiAgencyAccount();
    if (organisation != null) {
      String username = organisation.getAgencyAccountUsername();
      String password = organisation.getAgencyAccountPassword();
      DOIRegistrationAgency agency = organisation.getDoiRegistrationAgency();

      if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password) && agency != null) {
        return new RestJsonApiDataCiteService(cfg.getDataCiteUrl(), username, password);
      } else {
        throw new InvalidConfigException(TYPE.REGISTRATION_BAD_CONFIG,
          "DOI agency account for " + organisation.getName() + " is missing information!");
      }
    } else {
      LOG.debug("No DOI agency account has been added and activated yet to this IPT.");
    }
    return null;
  }

  @Override
  public Organisation addHostingOrganisation(Organisation organisation) {
    if (organisation != null) {
      LOG.debug("Adding hosting organisation {} - {}", organisation.getKey(), organisation.getName());
      registration.setHostingOrganisation(organisation);
    }
    return organisation;
  }

  @Override
  public void addIptInstance(Ipt ipt) {
    if (ipt != null) {
      if (ipt.getCreated() == null) {
        ipt.setCreated(new Date());
      }
      registration.setIpt(ipt);
    }
  }

  @Override
  public Organisation delete(String key, List<Resource> resources) throws DeletionNotAllowedException {
    Organisation org = get(key);
    if (org != null) {
      for (Resource resource : resources) {
        // Ensure the organisation is not associated to any registered resources
        if (resource.getOrganisation() != null && resource.getOrganisation().equals(org)) {
          throw new DeletionNotAllowedException(Reason.RESOURCE_REGISTERED_WITH_ORGANISATION,
            "Resource " + resource.getShortname() + " associated with organisation");
        }
        // Ensure the organisation is not associated to any resources with registered DOIs
        else if (resource.getDoiOrganisationKey() != null && resource.getDoiOrganisationKey().equals(org.getKey())) {
          throw new DeletionNotAllowedException(Reason.RESOURCE_DOI_REGISTERED_WITH_ORGANISATION,
            "Resource " + resource.getShortname() + " has DOI associated with organisation");
        }
      }
      // Check that the organization is not the hosting organization (IPT is not registered against the organization)
      Organisation host = registration.getHostingOrganisation();
      if (host != null && host.getKey() != null && host.getKey().toString().equals(key)) {
        throw new DeletionNotAllowedException(Reason.IPT_REGISTERED_WITH_ORGANISATION,
          "The IPT instance is associated with this organisation");
      }
      registration.getAssociatedOrganisations().remove(key);
    }
    return org;
  }

  @Override
  public Organisation get(String key) {
    if (key == null) {
      return null;
    }
    return registration.getAssociatedOrganisations().get(key);
  }

  @Override
  public Organisation get(UUID key) {
    if (key == null) {
      return null;
    }
    return registration.getAssociatedOrganisations().get(key.toString());
  }

  @Override
  public Organisation getHostingOrganisation() {
    return registration.getHostingOrganisation();
  }

  @Override
  public Network getNetwork() {
    return registration.getNetwork();
  }

  @Override
  public Ipt getIpt() {
    return registration.getIpt();
  }

  @Override
  public List<Organisation> list() {
    return registration.getAssociatedOrganisations().values().stream()
        .filter(Organisation::isCanHost)
        .sorted(ORG_BY_NAME_ORD)
        .collect(Collectors.toList());
  }

  @Override
  public List<Organisation> listAll() {
    return registration.getAssociatedOrganisations().values().stream()
        .sorted(ORG_BY_NAME_ORD)
        .collect(Collectors.toList());
  }

  @Override
  public void load() throws InvalidConfigException {
    registration.getAssociatedOrganisations().clear();
    try {
      Registration reg = store.load();
      if (reg == null) {
        LOG.warn("Registration information not existing, {} file missing  (This is normal when IPT is not registered yet)", PERSISTENCE_FILE);
      } else {
        // load the organisation this IPT is registered against
        addHostingOrganisation(reg.getHostingOrganisation());
        addIptInstance(reg.getIpt());
        registration.setNetwork(reg.getNetwork());
        // load the associated organisations
        for (Organisation org : reg.getAssociatedOrganisations().values()) {
          addAssociatedOrganisation(org);
        }
      }
    } catch (AlreadyExistingException e) {
      LOG.error(e);
    } catch (IOException | RuntimeException e) {
      LOG.error(e.getMessage(), e);
      throw new InvalidConfigException(TYPE.REGISTRATION_CONFIG,
        "Couldnt read the registration information: " + e.getMessage());
    }

    // it could be organisations have changed their name or node in the Registry, so update all organisation metadata
    try {
      updateAssociatedOrganisationsMetadata();
    } catch (IOException e) {
      LOG.error("Failed to update associated organisations", e);
    }
  }

  @Override
  public Organisation getFromDisk(String key) {
    try {
      Registration reg = store.load();
      return reg == null ? null : reg.getAssociatedOrganisations().get(key);
    } catch (IOException | RuntimeException e) {
      LOG.error(e.getMessage(), e);
      throw new InvalidConfigException(TYPE.REGISTRATION_CONFIG, "Couldnt read registration info: " + e.getMessage());
    }
  }

  /**
   * Update the metadata of each organization that has been added to the IPT with the latest version coming from the
   * Registry.
   */
  @Override
  public void updateAssociatedOrganisationsMetadata() throws IOException {
    // 1. update associated organisations' metadata
    for (Map.Entry<String, Organisation> entry : registration.getAssociatedOrganisations().entrySet()) {
      Organisation o = entry.getValue();
      updateOrganisationMetadata(o);
      // replace organisation in list of associated organisations now
      registration.getAssociatedOrganisations().put(entry.getKey(), o);
    }

    // 2. update hosting organisation's metadata
    Organisation hostingOrganisation = registration.getHostingOrganisation();
    if (hostingOrganisation != null) {
      updateOrganisationMetadata(hostingOrganisation);
    }

    // ensure changes are persisted to registration.json
    save();
  }

  /**
   * For a single organization, update its metadata. Only updates the metadata for an organisation coming from the
   * registry, not the metadata set by the IPT administrator like can host data, DOI configuration, etc.
   *
   * @param organisation Organisation
   */
  private void updateOrganisationMetadata(Organisation organisation) {
    if (organisation != null) {
      // the organization key
      String key = (organisation.getKey() == null) ? null : organisation.getKey().toString();

      // retrieve the latest copy of the organisation from the Registry
      Organisation o = registryManager.getRegisteredOrganisation(key);

      if (o != null) {

        String oKey = (o.getKey() == null) ? null : o.getKey().toString();
        String oName = StringUtils.trimToNull(o.getName());

        // sanity check - only the key must be exactly the same, and at least the name must not be null
        if (oKey != null && oKey.equalsIgnoreCase(key) && oName != null) {
          // organisation
          organisation.setName(oName);
          organisation.setDescription(StringUtils.trimToNull(o.getDescription()));
          organisation.setHomepageURL(StringUtils.trimToNull(o.getHomepageURL()));
          // organisation node
          organisation.setNodeKey(StringUtils.trimToNull(o.getNodeKey()));
          organisation.setNodeName(StringUtils.trimToNull(o.getNodeName()));
          organisation.setNodeContactEmail(StringUtils.trimToNull(o.getNodeContactEmail()));
          // organisation primary contact
          organisation.setPrimaryContactName(StringUtils.trimToNull(o.getPrimaryContactName()));
          organisation.setPrimaryContactFirstName(StringUtils.trimToNull(o.getPrimaryContactFirstName()));
          organisation.setPrimaryContactLastName(StringUtils.trimToNull(o.getPrimaryContactLastName()));
          organisation.setPrimaryContactAddress(StringUtils.trimToNull(o.getPrimaryContactAddress()));
          organisation.setPrimaryContactDescription(StringUtils.trimToNull(o.getPrimaryContactDescription()));
          organisation.setPrimaryContactEmail(StringUtils.trimToNull(o.getPrimaryContactEmail()));
          organisation.setPrimaryContactPhone(StringUtils.trimToNull(o.getPrimaryContactPhone()));
          organisation.setPrimaryContactType(StringUtils.trimToNull(o.getPrimaryContactType()));
          LOG.debug("Organisation ({}) updated with latest metadata from Registry", key);
        } else {
          LOG.debug("Update of organisation failed: organisation retrieved from Registry was missing name");
        }
      } else {
        LOG.debug("Update of organisation failed: organisation retrieved from Registry was null");
      }
    } else {
      LOG.debug("Update of organisation failed: organisation was null");
    }
  }

  @Override
  public void associateWithNetwork(String networkKey, String networkName) {
    try {
      Network network = registration.getNetwork();
      if (network != null) {
        network.setKey(networkKey);
        network.setName(networkName);
      } else {
        Network n = new Network();
        n.setName(networkName);
        n.setKey(networkKey);
        registration.setNetwork(n);
      }

      save();
    } catch (IOException e) {
      LOG.error("Failed to associate with the network");
    }
  }

  @Override
  public void removeAssociationWithNetwork() {
    try {
      registration.setNetwork(null);
      save();
    } catch (IOException e) {
      LOG.error("Failed to remove association with the network");
    }
  }

  @Override
  public synchronized void save() throws IOException {
    LOG.debug("Saving all user organisations associated to this IPT...");
    store.save(registration);
  }

}
