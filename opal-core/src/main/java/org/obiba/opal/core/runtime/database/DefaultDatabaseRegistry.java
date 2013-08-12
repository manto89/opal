package org.obiba.opal.core.runtime.database;

import java.sql.SQLException;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.obiba.opal.core.cfg.OpalConfigurationExtension;
import org.obiba.opal.core.domain.database.Database;
import org.obiba.opal.core.domain.database.SqlDatabase;
import org.obiba.opal.core.runtime.NoSuchServiceConfigurationException;
import org.obiba.opal.core.runtime.Service;
import org.obiba.opal.core.runtime.jdbc.DataSourceFactory;
import org.obiba.opal.core.runtime.jdbc.SessionFactoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

@Component
@Transactional
public class DefaultDatabaseRegistry implements DatabaseRegistry, Service {

  private static final Logger log = LoggerFactory.getLogger(DefaultDatabaseRegistry.class);

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private SessionFactoryFactory sessionFactoryFactory;

  @Autowired
  private DataSourceFactory dataSourceFactory;

  private final LoadingCache<String, BasicDataSource> dataSourceCache = CacheBuilder.newBuilder()
      .removalListener(new RemovalListener<String, BasicDataSource>() {

        @Override
        public void onRemoval(RemovalNotification<String, BasicDataSource> notification) {
          try {
            log.info("Destroying DataSource {}", notification.getKey());
            BasicDataSource dataSource = notification.getValue();
            if(dataSource != null) dataSource.close();
          } catch(SQLException e) {
            log.warn("Ignoring exception during shutdown: ", e);
          }
        }
      }) //
      .build(new CacheLoader<String, BasicDataSource>() {

        @Override
        public BasicDataSource load(String databaseName) throws Exception {
          log.info("Building DataSource {}", databaseName);
          return dataSourceFactory.createDataSource((SqlDatabase) getDatabase(databaseName));
        }
      });

  private final LoadingCache<String, SessionFactory> sessionFactoryCache = CacheBuilder.newBuilder()
      .removalListener(new RemovalListener<String, SessionFactory>() {
        @Override
        public void onRemoval(RemovalNotification<String, SessionFactory> notification) {
          try {
            log.info("Destroying session factory {}", notification.getKey());
            SessionFactory sf = notification.getValue();
            if(sf != null) sf.close();
          } catch(HibernateException e) {
            log.warn("Ignoring exception during shutdown: ", e);
          }
        }
      }) //
      .build(new CacheLoader<String, SessionFactory>() {

        @Override
        public SessionFactory load(String databaseName) throws Exception {
          log.info("Building SessionFactory {}", databaseName);
          return sessionFactoryFactory.getSessionFactory(getDataSource(databaseName, null));
        }
      });

  private final SetMultimap<String, String> registrations = Multimaps
      .synchronizedSetMultimap(HashMultimap.<String, String>create());

  @SuppressWarnings("unchecked")
  @Override
  public Iterable<Database> list() {
    return getCurrentSession().createCriteria(Database.class).list();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Database> Iterable<T> list(@Nonnull Class<T> databaseClass) {
    return getCurrentSession().createCriteria(databaseClass).list();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iterable<Database> list(@Nullable String type) {
    return Strings.isNullOrEmpty(type)
        ? list()
        : getCurrentSession().createCriteria(Database.class).add(Restrictions.eq("type", type)).list();
  }

  @Override
  public Database getDatabase(@Nonnull String name) {
    return (Database) getCurrentSession().createCriteria(Database.class).add(Restrictions.eq("name", name))
        .uniqueResult();
  }

  @Override
  public DataSource getDataSource(@Nonnull String name, @Nullable String usedByDatasource) {
    register(name, usedByDatasource);
    return dataSourceCache.getUnchecked(name);
  }

  @Override
  public SessionFactory getSessionFactory(@Nonnull String name, @Nullable String usedByDatasource) {
    register(name, usedByDatasource);
    return sessionFactoryCache.getUnchecked(name);
  }

  @Override
  public void addOrReplaceDatabase(@Nonnull Database database)
      throws DuplicateDatabaseNameException, MultipleIdentifiersDatabaseException {
    validUniqueName(database);
    validUniqueIdentifiersDatabase(database);
    getCurrentSession().update(database);
    destroyDataSource(database.getName());
  }

  private void validUniqueName(Database database) throws DuplicateDatabaseNameException {
    for(Database existing : list()) {
      if(database.getName().equalsIgnoreCase(existing.getName()) &&
          !Objects.equals(existing.getId(), database.getId())) {
        throw new DuplicateDatabaseNameException(database.getName());
      }
    }
  }

  private void validUniqueIdentifiersDatabase(Database database) throws MultipleIdentifiersDatabaseException {
    if(database.isUsedForIdentifiers()) {
      Database identifiersDatabase = getIdentifiersDatabase();
      if(identifiersDatabase != null && !Objects.equals(identifiersDatabase.getId(), database.getId())) {
        throw new MultipleIdentifiersDatabaseException(identifiersDatabase, database);
      }
    }
  }

  @Override
  public void deleteDatabase(@Nonnull Database database) {
    getCurrentSession().delete(database);
    destroyDataSource(database.getName());
  }

  private void destroyDataSource(String name) {
    sessionFactoryCache.invalidate(name);
    dataSourceCache.invalidate(name);
  }

  private void register(String databaseName, String usedByDatasource) {
    if(Strings.isNullOrEmpty(usedByDatasource)) return;
    Database database = getDatabase(databaseName);
    database.setEditable(false);
    addOrReplaceDatabase(database);
    registrations.put(databaseName, usedByDatasource);
  }

  @Override
  public void unregister(@Nonnull String databaseName, String usedByDatasource) {
    Database database = getDatabase(databaseName);
    database.setEditable(true);
    addOrReplaceDatabase(database);
    registrations.remove(databaseName, usedByDatasource);
  }

  @Nullable
  @Override
  public Database getIdentifiersDatabase() {
    return (Database) getCurrentSession().createCriteria(Database.class)
        .add(Restrictions.eq("usedForIdentifiers", true)).uniqueResult();
  }

  private Session getCurrentSession() {
    return sessionFactory.getCurrentSession();
  }

  @Override
  public boolean isRunning() {
    return true;
  }

  @Override
  public void start() {
  }

  @Override
  public void stop() {
    sessionFactoryCache.invalidateAll();
    dataSourceCache.invalidateAll();
  }

  @Override
  public String getName() {
    return "databases";
  }

  @Override
  public OpalConfigurationExtension getConfig() throws NoSuchServiceConfigurationException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}
