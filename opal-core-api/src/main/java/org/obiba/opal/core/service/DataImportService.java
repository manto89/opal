/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.core.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.apache.commons.vfs2.FileObject;
import org.obiba.magma.Datasource;
import org.obiba.magma.NoSuchDatasourceException;
import org.obiba.magma.NoSuchValueTableException;
import org.obiba.magma.ValueTable;
import org.obiba.opal.core.domain.participant.identifier.IParticipantIdentifier;
import org.obiba.opal.core.identifiers.IdentifiersMapping;
import org.obiba.opal.core.unit.FunctionalUnit;

/**
 * Service for import-related operations.
 */
public interface DataImportService {

  /**
   * Imports data into an Opal datasource .
   *
   * @param unitName functional unit name
   * @param sourceFile data file to be imported
   * @param destinationDatasourceName name of the destination datasource
   * @param allowIdentifierGeneration unknown participant will be created at importation time
   * @param ignoreUnknownIdentifier
   * @throws NoSuchDatasourceException if the specified datasource does not exist
   * @throws IllegalArgumentException if the specified file does not exist or is not a normal file
   * @throws IOException on any I/O error
   * @throws InterruptedException if the current thread was interrupted
   */
  void importData(@Nullable String unitName, @NotNull FileObject sourceFile, @NotNull String destinationDatasourceName,
      boolean allowIdentifierGeneration, boolean ignoreUnknownIdentifier)
      throws NoSuchFunctionalUnitException, NoSuchDatasourceException, IllegalArgumentException, IOException,
      InterruptedException;

  /**
   * Imports data from a source Opal datasource into a destination Opal datasource. Usually the source datasource will
   * be a transient datasource created temporarily when importing from a non-datasource source such as a csv file or
   * excel file.
   *
   * @param sourceDatasourceName name of the source datasource
   * @param destinationDatasourceName name of the destination datasource
   * @param allowIdentifierGeneration unknown participant will be created at importation time
   * @param ignoreUnknownIdentifier
   * @throws NoSuchFunctionalUnitException
   * @throws NonExistentVariableEntitiesException if unitName is null and the source entities do not exist as public
   * keys in the opal keys database
   * @throws IOException on any I/O error
   * @throws InterruptedException if the current thread was interrupted
   */
  void importData(String sourceDatasourceName, String destinationDatasourceName, boolean allowIdentifierGeneration,
      boolean ignoreUnknownIdentifier)
      throws NoSuchFunctionalUnitException, NoSuchDatasourceException, NoSuchValueTableException,
      NonExistentVariableEntitiesException, IOException, InterruptedException;

  /**
   * Imports data from a source Opal tables into a destination Opal datasource.
   *
   * @param sourceTableNames
   * @param destinationDatasourceName
   * @param allowIdentifierGeneration
   * @param ignoreUnknownIdentifier
   * @throws NoSuchFunctionalUnitException
   * @throws NonExistentVariableEntitiesException
   * @throws IOException
   * @throws InterruptedException
   */
  void importData(List<String> sourceTableNames, String destinationDatasourceName, boolean allowIdentifierGeneration,
      boolean ignoreUnknownIdentifier)
      throws NoSuchFunctionalUnitException, NoSuchDatasourceException, NoSuchValueTableException,
      NonExistentVariableEntitiesException, IOException, InterruptedException;

  /**
   * Imports data from a source table into a destination Opal datasource.
   *
   * @param sourceValueTables
   * @param destinationDatasourceName
   * @param allowIdentifierGeneration
   * @param ignoreUnknownIdentifier
   */
  void importData(Set<ValueTable> sourceValueTables, String destinationDatasourceName,
      boolean allowIdentifierGeneration, boolean ignoreUnknownIdentifier)
      throws NoSuchFunctionalUnitException, NonExistentVariableEntitiesException, IOException, InterruptedException;

}