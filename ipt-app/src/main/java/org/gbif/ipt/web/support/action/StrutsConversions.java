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
package org.gbif.ipt.web.support.action;

import org.gbif.ipt.model.datapackage.metadata.DataPackageMetadata;
import org.gbif.ipt.model.datapackage.metadata.camtrap.CamtrapLicense;
import org.gbif.ipt.model.datapackage.metadata.camtrap.CamtrapMetadata;
import org.gbif.ipt.model.datapackage.metadata.camtrap.CamtrapSource;
import org.gbif.ipt.model.datapackage.metadata.camtrap.Geojson;
import org.gbif.ipt.model.datapackage.metadata.camtrap.Project;
import org.gbif.ipt.model.datapackage.metadata.camtrap.Taxonomic;
import org.gbif.ipt.model.datapackage.metadata.camtrap.Temporal;
import org.gbif.metadata.eml.ipt.model.Eml;
import org.gbif.metadata.eml.ipt.model.Point;
import org.gbif.metadata.eml.ipt.model.TemporalCoverage;
import org.gbif.metadata.eml.ipt.util.DateUtils;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.support.GenericConversionService;

/**
 * IPT 3's struts2.converter classes for the properties its {@code *-conversion.properties} files named. Those files
 * for Eml, Point and TemporalCoverage sat under a package the metadata library had left, so IPT 3 at the baseline fell
 * back to Struts' default conversion there; IPT4 applies the converters as intended. The files for classes the
 * baseline no longer had (Contributor, DataPackageLicense, DataPackageSource) are left out.
 */
public final class StrutsConversions implements ConditionalGenericConverter {

  /** Only these properties: everything else keeps Spring's default conversion. */
  public static final ConversionService SERVICE = service();

  private record Rule(Class<?> owner, Set<String> properties, Function<String, Object> convert) {
  }

  private static final List<Rule> RULES = List.of(
      new Rule(Eml.class, Set.of("pubDate"), StrutsConversions::date),
      new Rule(TemporalCoverage.class, Set.of("startDate", "endDate"), StrutsConversions::date),
      new Rule(Point.class, Set.of("latitude"), value -> coordinate(value, 90)),
      new Rule(Point.class, Set.of("longitude"), value -> coordinate(value, 180)),
      new Rule(DataPackageMetadata.class, Set.of("homepage"), StrutsConversions::uri),
      new Rule(DataPackageMetadata.class, Set.of("title", "description", "image"), StrutsConversions::emptyToNull),
      new Rule(CamtrapMetadata.class, Set.of("homepage"), StrutsConversions::uri),
      new Rule(CamtrapLicense.class, Set.of("name", "path", "title"), StrutsConversions::emptyToNull),
      new Rule(CamtrapSource.class, Set.of("version"), StrutsConversions::emptyToNull),
      new Rule(Project.class, Set.of("title", "acronym", "description"), StrutsConversions::emptyToNull),
      new Rule(Taxonomic.class, Set.of("taxonID", "scientificName"), StrutsConversions::emptyToNull),
      new Rule(Temporal.class, Set.of("start", "end"), StrutsConversions::emptyToNull),
      new Rule(Geojson.class, Set.of("type"), value -> StringUtils.isEmpty(value) ? null : Geojson.Type.fromValue(value)));

  private static ConversionService service() {
    GenericConversionService service = new GenericConversionService();
    service.addConverter(new StrutsConversions());
    return service;
  }

  private StrutsConversions() {
  }

  @Override
  public Set<ConvertiblePair> getConvertibleTypes() {
    return Set.of(new ConvertiblePair(String.class, Object.class));
  }

  @Override
  public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return rule(targetType) != null;
  }

  @Override
  @Nullable
  public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
    Rule rule = rule(targetType);
    return source == null || rule == null ? null : rule.convert().apply((String) source);
  }

  /** The rule for the bean property the target describes (Spring describes properties by their setter). */
  @Nullable
  private static Rule rule(TypeDescriptor targetType) {
    if (!(targetType.getSource() instanceof MethodParameter parameter) || parameter.getMethod() == null) {
      return null;
    }
    Method method = parameter.getMethod();
    String name = method.getName();
    if (!name.startsWith("set") && !name.startsWith("get") && !name.startsWith("is")) {
      return null;
    }
    String property = Introspector.decapitalize(name.substring(name.startsWith("is") ? 2 : 3));
    Class<?> owner = parameter.getContainingClass();
    for (Rule rule : RULES) {
      if (rule.owner().isAssignableFrom(owner) && rule.properties().contains(property)) {
        return rule;
      }
    }
    return null;
  }

  /** DateFormatConverter: a year alone is that year plus one millisecond (EmlFactory#calendarDate). */
  @Nullable
  static Date date(String value) {
    if (StringUtils.isEmpty(value)) {
      return null;
    }
    if (value.length() == 4) {
      try {
        return new Date(new SimpleDateFormat("yyyy").parse(value).getTime() + 1);
      } catch (ParseException e) {
        // not a year: try the other formats
      }
    }
    return DateUtils.parse(value);
  }

  /** CoordinateFormatConverter: a decimal (also with a comma) within the coordinate's range. */
  @Nullable
  static Double coordinate(String value, double limit) {
    if (value.isEmpty()) {
      return null;
    }
    Double number;
    try {
      number = Double.parseDouble(value);
    } catch (NumberFormatException e) {
      DecimalFormatSymbols symbols = new DecimalFormatSymbols();
      symbols.setDecimalSeparator(',');
      ParsePosition position = new ParsePosition(0);
      Number parsed = new DecimalFormat("###.##", symbols).parse(value, position);
      if (parsed == null || position.getIndex() != value.length()) {
        throw new IllegalArgumentException("Invalid decimal number: " + value);
      }
      number = parsed.doubleValue();
    }
    if (number < -limit || number > limit) {
      throw new IllegalArgumentException("Invalid decimal number: " + value);
    }
    return number;
  }

  @Nullable
  static URI uri(String value) {
    return StringUtils.isEmpty(value) ? null : URI.create(value);
  }

  @Nullable
  static String emptyToNull(String value) {
    return StringUtils.trimToNull(value);
  }
}
