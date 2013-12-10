/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.navigator.event;

import javax.annotation.Nullable;

import org.obiba.opal.web.model.client.magma.TableDto;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Event to indicate that a Magma Table has been selected.
 */
public class TableSelectionChangeEvent extends GwtEvent<TableSelectionChangeEvent.Handler> {

  public interface Handler extends EventHandler {

    void onTableSelectionChanged(TableSelectionChangeEvent event);

  }

  private static final Type<Handler> TYPE = new Type<Handler>();

  private final String datasourceName;

  private final String tableName;

  private final String previous;

  private final String next;

  private final Object source;

  public TableSelectionChangeEvent(Object source, String datasourceName, String tableName) {
    this(source, datasourceName, tableName, null, null);
  }

  public TableSelectionChangeEvent(Object source, String datasourceName, String tableName, @Nullable String previous, @Nullable String next) {
    this.source = source;
    this.datasourceName = datasourceName;
    this.tableName = tableName;
    this.previous = previous;
    this.next = next;
  }

  @Override
  public Object getSource() {
    return source;
  }

  public String getDatasourceName() {
    return datasourceName;
  }

  public String getTableName() {
    return tableName;
  }

  public String getPrevious() {
    return previous;
  }

  public String getNext() {
    return next;
  }

  public static Type<Handler> getType() {
    return TYPE;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onTableSelectionChanged(this);
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return getType();
  }
}