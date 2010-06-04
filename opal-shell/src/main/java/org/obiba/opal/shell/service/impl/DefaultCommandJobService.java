/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.shell.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.obiba.magma.audit.UserProvider;
import org.obiba.opal.shell.CommandJob;
import org.obiba.opal.shell.service.CommandJobService;
import org.obiba.opal.shell.service.NoSuchCommandJobException;
import org.obiba.opal.web.model.Commands.CommandStateDto.Status;

/**
 * Default implementation of {@link CommandJobService}.
 */
public class DefaultCommandJobService implements CommandJobService {
  //
  // Instance Variables
  //

  private ExecutorService executor;

  private UserProvider userProvider;

  private boolean isRunning;

  private List<CommandJob> history;

  private long lastJobId;

  //
  // Constructors
  //

  public DefaultCommandJobService() {
    history = Collections.synchronizedList(new ArrayList<CommandJob>());

    // TODO: Inject this dependency.
    executor = Executors.newFixedThreadPool(10);
  }

  //
  // Service Methods
  //

  public void start() {
    isRunning = true;
  }

  public void stop() {
    isRunning = false;
  }

  public boolean isRunning() {
    return isRunning;
  }

  //
  // CommandJobService Methods
  //

  public Long launchCommand(CommandJob commandJob) {
    Long id = nextJobId();

    commandJob.setId(id);
    commandJob.setOwner(userProvider.getUsername());
    commandJob.setSubmitTime(getCurrentTime());

    executor.execute(commandJob);

    history.add(0, commandJob);

    return id;
  }

  public CommandJob getCommand(Long id) {
    for(CommandJob job : getHistory()) {
      if(job.getId().equals(id)) {
        return job;
      }
    }
    return null;
  }

  public List<CommandJob> getHistory() {
    return Collections.unmodifiableList(history);
  }

  public void deleteCommand(Long id) {
    for(int i = 0; i < history.size(); i++) {
      CommandJob job = history.get(i);
      if(job.getId().equals(id)) {
        if(isRunning(job)) {
          throw new IllegalStateException("commandJob is running");
        }
        history.remove(i);
        return;
      }
    }
    throw new NoSuchCommandJobException(id);
  }

  //
  // Methods
  //

  public void setExecutorService(ExecutorService executor) {
    this.executor = executor;
  }

  public void setUserProvider(UserProvider userProvider) {
    this.userProvider = userProvider;
  }

  /**
   * Generates an id for a {@link CommandJob}.
   * 
   * The sequence 1, 2, 3, ..., is returned.
   * 
   * @return an id for a {@link CommandJob}
   */
  protected synchronized Long nextJobId() {
    return ++lastJobId;
  }

  protected Date getCurrentTime() {
    return new Date();
  }

  private boolean isRunning(CommandJob commandJob) {
    return commandJob.getStatus().equals(Status.IN_PROGRESS) || commandJob.getStatus().equals(Status.CANCEL_PENDING);
  }
}
