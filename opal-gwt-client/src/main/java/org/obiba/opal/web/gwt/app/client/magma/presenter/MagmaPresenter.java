package org.obiba.opal.web.gwt.app.client.magma.presenter;

import javax.inject.Provider;

import org.obiba.opal.web.gwt.app.client.bookmark.presenter.BookmarkIconPresenter;
import org.obiba.opal.web.gwt.app.client.event.NotificationEvent;
import org.obiba.opal.web.gwt.app.client.js.JsArrays;
import org.obiba.opal.web.gwt.app.client.magma.event.DatasourceSelectionChangeEvent;
import org.obiba.opal.web.gwt.app.client.magma.event.MagmaPathSelectionEvent;
import org.obiba.opal.web.gwt.app.client.magma.event.TableSelectionChangeEvent;
import org.obiba.opal.web.gwt.app.client.magma.event.VariableSelectionChangeEvent;
import org.obiba.opal.web.gwt.app.client.support.MagmaPath;
import org.obiba.opal.web.gwt.rest.client.ResourceCallback;
import org.obiba.opal.web.gwt.rest.client.ResourceRequestBuilderFactory;
import org.obiba.opal.web.gwt.rest.client.ResponseCodeCallback;
import org.obiba.opal.web.gwt.rest.client.UriBuilders;
import org.obiba.opal.web.model.client.magma.TableDto;
import org.obiba.opal.web.model.client.magma.VariableDto;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.PresenterWidget;
import com.gwtplatform.mvp.client.View;

public class MagmaPresenter extends PresenterWidget<MagmaPresenter.Display>
    implements MagmaPathSelectionEvent.Handler, DatasourceSelectionChangeEvent.DatasourceSelectionChangeHandler,
    TableSelectionChangeEvent.Handler, VariableSelectionChangeEvent.Handler {

  private final DatasourcePresenter datasourcePresenter;

  private final TablePresenter tablePresenter;

  private final VariablePresenter variablePresenter;

  private final BookmarkIconPresenter bookmarkIconPresenter;

  @Inject
  public MagmaPresenter(EventBus eventBus, Display display, DatasourcePresenter datasourcePresenter,
      TablePresenter tablePresenter, VariablePresenter variablePresenter,
      Provider<BookmarkIconPresenter> bookmarkIconPresenterProvider) {
    super(eventBus, display);
    this.datasourcePresenter = datasourcePresenter;
    this.tablePresenter = tablePresenter;
    this.variablePresenter = variablePresenter;
    bookmarkIconPresenter = bookmarkIconPresenterProvider.get();
  }

  @Override
  protected void onBind() {
    super.onBind();

    addRegisteredHandler(MagmaPathSelectionEvent.getType(), this);
    addRegisteredHandler(DatasourceSelectionChangeEvent.getType(), this);
    addRegisteredHandler(TableSelectionChangeEvent.getType(), this);
    addRegisteredHandler(VariableSelectionChangeEvent.getType(), this);

    setInSlot(Display.Slot.DATASOURCE, datasourcePresenter);
    setInSlot(Display.Slot.TABLE, tablePresenter);
    setInSlot(Display.Slot.VARIABLE, variablePresenter);

    getView().setBookmarkIcon(bookmarkIconPresenter);
  }

  @Override
  public void onDatasourceSelectionChange(DatasourceSelectionChangeEvent event) {
    if(!equals(event.getSource())) {
      getView().selectDatasource(event.getDatasource());
    }
  }

  @Override
  public void onTableSelectionChanged(TableSelectionChangeEvent event) {
    if(!equals(event.getSource())) {
      getView().selectTable(event.getDatasourceName(), event.getTableName(), event.isView());
    }
  }

  @Override
  public void onVariableSelectionChanged(VariableSelectionChangeEvent event) {
    getView().selectVariable(event.getDatasourceName(), event.getTableName(), event.getVariableName());
  }

  @Override
  public void onMagmaPathSelection(MagmaPathSelectionEvent event) {
    if(event.getSource() == this) return;

    MagmaPath.Parser parser = event.getParser();
    if(parser.hasVariable()) {
      show(parser.getDatasource(), parser.getTable(), parser.getVariable());
    } else if(parser.hasTable()) {
      show(parser.getDatasource(), parser.getTable());
    } else {
      show(parser.getDatasource());
    }
  }

  private void show(String datasource) {
    getView().selectDatasource(datasource);
    fireEvent(new DatasourceSelectionChangeEvent(this, datasource));
    bookmarkIconPresenter.setBookmarkable(null);
  }

  private void show(String datasource, String table) {
    getView().selectTable(datasource, table, false);
    fireEvent(new TableSelectionChangeEvent(this, datasource, table));
    bookmarkIconPresenter.setBookmarkable(UriBuilders.DATASOURCE_TABLE.create().build(datasource, table));
  }

  private void show(final String datasource, final String table, final String variable) {
    // table counts are required for having variable summary and values
    ResourceRequestBuilderFactory.<TableDto>newBuilder() //
        .forResource(UriBuilders.DATASOURCE_TABLE.create().query("counts", "true").build(datasource, table)) //
        .withCallback(new ResourceCallback<TableDto>() {
          @Override
          public void onResource(Response response, final TableDto tableDto) {
            if(tableDto == null) return;

            ResourceRequestBuilderFactory.<JsArray<VariableDto>>newBuilder() //
                .forResource(UriBuilders.DATASOURCE_TABLE_VARIABLES.create().build(datasource, table)) //
                .withCallback(new ResourceCallback<JsArray<VariableDto>>() {

                  @Override
                  public void onResource(Response response, JsArray<VariableDto> resource) {
                    JsArray<VariableDto> variables = JsArrays.toSafeArray(resource);
                    VariableDto previous = null;
                    VariableDto selection = null;
                    VariableDto next = null;
                    int nbVariables = variables.length();
                    for(int i = 0; i < nbVariables; i++) {
                      if(variables.get(i).getName().equals(variable)) {
                        selection = variables.get(i);
                        if(i >= 0) {
                          previous = variables.get(i - 1);
                        }
                        if(i < nbVariables - 1) {
                          next = variables.get(i + 1);
                        }
                        break;
                      }
                    }
                    fireEvent(new VariableSelectionChangeEvent(tableDto, selection, previous, next));
                  }
                })//
                .withCallback(Response.SC_SERVICE_UNAVAILABLE, new ResponseCodeCallback() {
                  @Override
                  public void onResponseCode(Request request, Response response) {
                    // TODO fix error message
                    getEventBus().fireEvent(NotificationEvent.newBuilder().error("SearchServiceUnavailable").build());
                  }
                }) //
                .get() //
                .send();
          }
        }) //
        .get() //
        .send();
    bookmarkIconPresenter
        .setBookmarkable(UriBuilders.DATASOURCE_TABLE_VARIABLE.create().build(datasource, table, variable));
  }

  public interface Display extends View {

    enum Slot {
      DATASOURCE,
      TABLE,
      VARIABLE,
    }

    void setBookmarkIcon(IsWidget widget);

    void selectDatasource(String name);

    void selectTable(String datasource, String table, boolean isView);

    void selectVariable(String datasource, String table, String variable);
  }

}
