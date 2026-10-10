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
package org.gbif.ipt.web.support.freemarker;

import java.util.Map;
import java.util.Set;

import freemarker.core.CollectionAndSequence;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.MapModel;
import freemarker.ext.util.ModelFactory;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.Version;

/**
 * Equivalent of Struts' {@code StrutsBeanWrapper} with {@code struts.freemarker.wrapper.altMap=true}: a plain
 * {@link BeansWrapper} that exposes maps as {@link FriendlyMapModel}, so templates can use {@code map.get(key)},
 * {@code map[key]}, {@code map.key} and {@code ?keys} exactly as in IPT 3.
 */
public class IptObjectWrapper extends BeansWrapper {

  public IptObjectWrapper(Version incompatibleImprovements) {
    super(incompatibleImprovements);
  }

  @Override
  protected ModelFactory getModelFactory(Class<?> clazz) {
    if (Map.class.isAssignableFrom(clazz)) {
      return FriendlyMapModel.FACTORY;
    }
    return super.getModelFactory(clazz);
  }

  /**
   * Port of Struts' {@code StrutsBeanWrapper.FriendlyMapModel}: a {@link MapModel} (so map methods stay callable) whose
   * keys, values and emptiness are those of the map, not of the bean properties.
   */
  static final class FriendlyMapModel extends MapModel implements TemplateHashModelEx {

    static final ModelFactory FACTORY =
        (object, wrapper) -> new FriendlyMapModel((Map<?, ?>) object, (BeansWrapper) wrapper);

    FriendlyMapModel(Map<?, ?> map, BeansWrapper wrapper) {
      super(map, wrapper);
    }

    @Override
    public boolean isEmpty() {
      return ((Map<?, ?>) object).isEmpty();
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected Set keySet() {
      return ((Map<?, ?>) object).keySet();
    }

    @Override
    public TemplateCollectionModel values() {
      return new CollectionAndSequence(new SimpleSequence(((Map<?, ?>) object).values(), wrapper));
    }
  }
}
