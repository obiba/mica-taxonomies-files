/*
 * Copyright (c) 2022 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.taxonomies.files;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.spi.taxonomies.AbstractTaxonomiesProviderService;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;


public class TaxonomiesFilesProviderService extends AbstractTaxonomiesProviderService {

  private static final Logger log = LoggerFactory.getLogger(TaxonomiesFilesProviderService.class);

  private ObjectMapper mapper;

  @Override
  public String getName() {
    return "mica-taxonomies-files";
  }

  @Override
  public boolean isFor(TaxonomyTarget target) {
    return TaxonomyTarget.VARIABLE.equals(target);
  }

  @Override
  public void start() {
    mapper = new ObjectMapper();
    super.start();
  }

  @Override
  public List<Taxonomy> getTaxonomies() {
    List<Taxonomy> taxonomies = Lists.newArrayList();
    File dataDir = Paths.get(properties.getProperty("data.dir")).toFile();
    if (dataDir.exists()) {
      File[] yamlFiles = dataDir.listFiles(file -> !file.isDirectory() && file.getName().endsWith(".yml"));
      if (yamlFiles != null) {
        for (File yamlFile : yamlFiles) {
          try {
            Taxonomy taxonomy = readFile(yamlFile.getAbsolutePath());
            taxonomies.add(taxonomy);
          } catch (Exception e) {
            log.error("Taxonomy file could not be read: {}", yamlFile.getAbsolutePath(), e);
          }
        }
      }
    }
    return taxonomies;
  }

  private Taxonomy readFile(String resourcePath) {
    YamlMapFactoryBean factory = new YamlMapFactoryBean();
    factory.setResources(new FileSystemResource(resourcePath));

    return mapper.convertValue(factory.getObject(), Taxonomy.class);
  }
}
