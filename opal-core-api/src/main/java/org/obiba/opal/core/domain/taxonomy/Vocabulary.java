/*
 * Copyright (c) 2013 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.opal.core.domain.taxonomy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.obiba.opal.core.domain.AbstractTimestamped;
import org.obiba.opal.core.domain.HasUniqueProperties;

import com.google.common.collect.Lists;

public class Vocabulary extends AbstractTimestamped implements HasUniqueProperties {

  @NotNull
  @NotBlank
  private String taxonomy;

  @NotNull
  @NotBlank
  private String name;

  private boolean repeatable;

  private Map<Locale, String> titles = new HashMap<>();

  private Map<Locale, String> descriptions = new HashMap<>();

  private List<Term> terms = new ArrayList<>();

  public Vocabulary() {
  }

  public Vocabulary(@NotNull String taxonomy, @NotNull String name) {
    this.taxonomy = taxonomy;
    this.name = name;
  }

  @Override
  public List<String> getUniqueProperties() {
    return Lists.newArrayList("taxonomy", "name");
  }

  @Override
  public List<Object> getUniqueValues() {
    return Lists.<Object>newArrayList(taxonomy, name);
  }

  @NotNull
  public String getTaxonomy() {
    return taxonomy;
  }

  public void setTaxonomy(@NotNull String taxonomy) {
    this.taxonomy = taxonomy;
  }

  @NotNull
  public String getName() {
    return name;
  }

  public void setName(@NotNull String name) {
    this.name = name;
  }

  public boolean isRepeatable() {
    return repeatable;
  }

  public void setRepeatable(boolean repeatable) {
    this.repeatable = repeatable;
  }

  public Map<Locale, String> getTitles() {
    return titles;
  }

  public void setTitles(Map<Locale, String> titles) {
    this.titles = titles;
  }

  public Vocabulary addTitle(Locale locale, String title) {
    if(titles == null) titles = new HashMap<>();
    titles.put(locale, title);
    return this;
  }

  public Map<Locale, String> getDescriptions() {
    return descriptions;
  }

  public void setDescriptions(Map<Locale, String> descriptions) {
    this.descriptions = descriptions;
  }

  public Vocabulary addDescription(Locale locale, String title) {
    if(descriptions == null) descriptions = new HashMap<>();
    descriptions.put(locale, title);
    return this;
  }

  public List<Term> getTerms() {
    return terms;
  }

  public void setTerms(List<Term> terms) {
    this.terms = terms;
  }

  public Vocabulary addTerm(Term term) {
    if(terms == null) terms = new ArrayList<>();
    if(terms.contains(term)) terms.remove(term);
    terms.add(term);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if(this == o) return true;
    if(!(o instanceof Vocabulary)) return false;
    Vocabulary that = (Vocabulary) o;
    return name.equals(that.name) && taxonomy.equals(that.taxonomy);
  }

  @Override
  public int hashCode() {
    int result = taxonomy.hashCode();
    result = 31 * result + name.hashCode();
    return result;
  }
}