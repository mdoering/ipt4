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

import org.gbif.ipt.model.datapackage.metadata.FrictionlessMetadata;
import org.gbif.ipt.model.datapackage.metadata.camtrap.Geojson;
import org.gbif.ipt.model.datapackage.metadata.camtrap.Project;
import org.gbif.metadata.eml.ipt.model.Eml;
import org.gbif.metadata.eml.ipt.model.GeospatialCoverage;
import org.gbif.metadata.eml.ipt.model.TemporalCoverage;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.DataBinder;

import static org.assertj.core.api.Assertions.assertThat;

/** IPT 3's struts2.converter classes, applied to the properties its *-conversion.properties named. */
class StrutsConversionsTest {

  public static class Form {
    private final Eml eml = new Eml();
    private final FrictionlessMetadata<?, ?, ?> metadata = new FrictionlessMetadata<>();
    private final Project project = new Project();
    private final Geojson geojson = new Geojson();

    public Eml getEml() {
      return eml;
    }

    public FrictionlessMetadata<?, ?, ?> getMetadata() {
      return metadata;
    }

    public Project getProject() {
      return project;
    }

    public Geojson getGeojson() {
      return geojson;
    }
  }

  private static DataBinder bind(Form form, MutablePropertyValues values) {
    DataBinder binder = new DataBinder(form);
    binder.setConversionService(StrutsConversions.SERVICE);
    binder.setAutoGrowNestedPaths(true);
    binder.bind(values);
    return binder;
  }

  @Test
  void datesInIpt3sFormatsAndYearsOnly() {
    Form form = new Form();
    form.getEml().setTemporalCoverages(new ArrayList<>(List.of(new TemporalCoverage())));
    MutablePropertyValues values = new MutablePropertyValues();
    values.add("eml.pubDate", "2020-03-04");
    values.add("eml.temporalCoverages[0].startDate", "1999");
    values.add("eml.temporalCoverages[0].endDate", "");
    assertThat(bind(form, values).getBindingResult().hasErrors()).isFalse();
    assertThat(new SimpleDateFormat("yyyy-MM-dd").format(form.getEml().getPubDate())).isEqualTo("2020-03-04");
    // IPT 3's year-only dates carry one millisecond (EmlFactory#calendarDate)
    assertThat(new SimpleDateFormat("yyyy SSS").format(form.getEml().getTemporalCoverages().get(0).getStartDate()))
        .isEqualTo("1999 001");
    assertThat(form.getEml().getTemporalCoverages().get(0).getEndDate()).isNull();
  }

  @Test
  void coordinatesWithCommaDecimalsAndRanges() {
    Form form = new Form();
    form.getEml().setGeospatialCoverages(new ArrayList<>(List.of(new GeospatialCoverage())));
    MutablePropertyValues values = new MutablePropertyValues();
    values.add("eml.geospatialCoverages[0].boundingCoordinates.min.latitude", "45,5");
    values.add("eml.geospatialCoverages[0].boundingCoordinates.min.longitude", "181");
    values.add("eml.geospatialCoverages[0].boundingCoordinates.max.latitude", "");
    DataBinder binder = bind(form, values);
    assertThat(form.getEml().getGeospatialCoverages().get(0).getBoundingCoordinates().getMin().getLatitude())
        .isEqualTo(45.5);
    assertThat(form.getEml().getGeospatialCoverages().get(0).getBoundingCoordinates().getMax().getLatitude()).isNull();
    assertThat(binder.getBindingResult().getFieldErrors())
        .extracting(e -> e.getField()).containsExactly("eml.geospatialCoverages[0].boundingCoordinates.min.longitude");
  }

  @Test
  void dataPackageFields() {
    Form form = new Form();
    MutablePropertyValues values = new MutablePropertyValues();
    values.add("metadata.homepage", "https://www.gbif.org");
    values.add("metadata.title", "");
    values.add("project.acronym", "  ");
    values.add("project.title", " Camera traps ");
    values.add("geojson.type", "MultiPolygon");
    assertThat(bind(form, values).getBindingResult().hasErrors()).isFalse();
    assertThat(form.getMetadata().getHomepage()).isEqualTo(URI.create("https://www.gbif.org"));
    assertThat(form.getMetadata().getTitle()).isNull();
    assertThat(form.getProject().getAcronym()).isNull();
    assertThat(form.getProject().getTitle()).isEqualTo("Camera traps");
    assertThat(form.getGeojson().getType()).isEqualTo(Geojson.Type.fromValue("MultiPolygon"));
  }
}
