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
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.obiba.mica.spi.search.TaxonomyTarget;
import org.obiba.mica.spi.taxonomies.AbstractTaxonomiesProviderService;
import org.obiba.mica.spi.taxonomies.TaxonomyImportException;
import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
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
    File dataDir = Paths.get(properties.getProperty("data.dir")).toFile();
    List<Taxonomy> taxonomies = getTaxonomies(dataDir);
    String files = properties.getProperty("files");
    if (!Strings.isNullOrEmpty(files)) {
      for (String path : Splitter.on(",").trimResults().split(files)) {
        File file = Paths.get(path).toFile();
        taxonomies.addAll(getTaxonomies(file));
      }
    }
    String urls = properties.getProperty("urls");
    if (!Strings.isNullOrEmpty(urls)) {
      for (String url : Splitter.on(",").trimResults().split(urls)) {
        try {
          taxonomies.add(getTaxonomy(new URL(url)));
        } catch (Exception e) {
          log.warn("Taxonomy URL '{}' cannot be read: {}", url, e.getMessage());
        }
      }
    }
    // note: if there are several taxonomies with same name, Mica will handle that
    return taxonomies;
  }

  /**
   * Load YAML files directly or lookup from directory, recursively.
   *
   * @param source
   * @return
   */
  @NotNull
  private List<Taxonomy> getTaxonomies(File source) {
    List<Taxonomy> taxonomies = Lists.newArrayList();
    if (!source.exists()) return taxonomies;

    if (source.isDirectory()) {
      File[] children = source.listFiles(file -> file.isDirectory() || file.getName().toLowerCase().endsWith(".yml"));
      if (children != null) {
        for (File child : children) {
          if (child.isDirectory()) {
            // recursive lookup
            taxonomies.addAll(getTaxonomies(child));
          } else {
            Taxonomy taxonomy = readFile(child);
            if (taxonomy != null)
              taxonomies.add(taxonomy);
          }
        }
      }
    } else if (source.getName().toLowerCase().endsWith(".yml")) {
      Taxonomy taxonomy = readFile(source);
      if (taxonomy != null)
        taxonomies.add(taxonomy);
    }

    return taxonomies;
  }

  private Taxonomy getTaxonomy(URL source) {
    return readTaxonomy(source);
  }

  /**
   * Load taxonomy from file, safely (only report errors in log).
   *
   * @param yamlFile
   * @return
   */
  private Taxonomy readFile(File yamlFile) {
    try {
      return readTaxonomy(yamlFile.toURI().toURL());
    } catch (MalformedURLException e) {
      log.error("Taxonomy file could not be read: {}", yamlFile.getAbsolutePath(), e);
      throw new TaxonomyImportException(e);
    }
  }
}
