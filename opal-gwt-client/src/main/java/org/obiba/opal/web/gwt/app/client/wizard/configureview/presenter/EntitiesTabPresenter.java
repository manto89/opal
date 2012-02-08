/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.wizard.configureview.presenter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.obiba.opal.web.gwt.app.client.event.NotificationEvent;
import org.obiba.opal.web.gwt.app.client.navigator.event.ViewConfigurationRequiredEvent;
import org.obiba.opal.web.gwt.app.client.validator.FieldValidator;
import org.obiba.opal.web.gwt.app.client.wizard.configureview.event.ViewSavePendingEvent;
import org.obiba.opal.web.gwt.app.client.wizard.configureview.event.ViewSaveRequiredEvent;
import org.obiba.opal.web.gwt.app.client.wizard.configureview.event.ViewSavedEvent;
import org.obiba.opal.web.gwt.app.client.wizard.createview.presenter.EvaluateScriptPresenter;
import org.obiba.opal.web.model.client.magma.JavaScriptViewDto;
import org.obiba.opal.web.model.client.magma.TableDto;
import org.obiba.opal.web.model.client.magma.VariableListViewDto;
import org.obiba.opal.web.model.client.magma.ViewDto;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.ListBox;
import com.google.inject.Inject;

public class EntitiesTabPresenter extends WidgetPresenter<EntitiesTabPresenter.Display> {

  public interface Display extends WidgetDisplay {

    void saveChangesEnabled(boolean enabled);

    void setScriptWidget(EvaluateScriptPresenter.Display scriptWidgetDisplay);

    void setScriptWidgetVisible(boolean visible);

    void setScript(String script);

    String getScript();

    void setEntitiesToView(EntitiesToView scriptOrAll);

    EntitiesToView getEntitiesToView();

    HandlerRegistration addSaveChangesClickHandler(ClickHandler clickHandler);

    HandlerRegistration addEntitiestoViewChangeHandler(ChangeHandler changeHandler);

    HandlerRegistration addScriptChangeHandler(ChangeHandler handler);

    ListBox getEntitiesToViewListBox();
  }

  public enum EntitiesToView {
    SCRIPT, ALL
  }

  /**
   * The {@link ViewDto} of the view being configured.
   * 
   * When the tab's save button is pressed, changes are applied to this ViewDto.
   */
  private ViewDto viewDto;

  private Set<FieldValidator> validators = new LinkedHashSet<FieldValidator>();

  /**
   * Widget for entering, and testing, the "select" script.
   */
  @Inject
  private EvaluateScriptPresenter scriptWidget;

  @Inject
  public EntitiesTabPresenter(final Display display, final EventBus eventBus) {
    super(display, eventBus);
  }

  @Override
  protected void onBind() {
    scriptWidget.bind();
    scriptWidget.showTest(false);
    getDisplay().setScriptWidget(scriptWidget.getDisplay());

    getDisplay().saveChangesEnabled(true);
    addEventHandlers();
  }

  @Override
  protected void onUnbind() {
    scriptWidget.unbind();
  }

  @Override
  public void revealDisplay() {
  }

  @Override
  public void refreshDisplay() {
    getDisplay().saveChangesEnabled(false);
  }

  @Override
  public Place getPlace() {
    return null;
  }

  @Override
  protected void onPlaceRequest(PlaceRequest request) {
  }

  private void addEventHandlers() {
    super.registerHandler(eventBus.addHandler(ViewConfigurationRequiredEvent.getType(), new ViewConfigurationRequiredEventHandler()));
    super.registerHandler(getDisplay().addSaveChangesClickHandler(new SaveChangesClickHandler()));
    super.registerHandler(getDisplay().addEntitiestoViewChangeHandler(new EntitiesToViewChangeHandler()));
    super.registerHandler(getDisplay().addEntitiestoViewChangeHandler(new FormChangedHandler()));
    super.registerHandler(getDisplay().addScriptChangeHandler(new FormChangedHandler()));
    super.registerHandler(eventBus.addHandler(ViewSavedEvent.getType(), new ViewSavedHandler()));
  }

  private boolean validate() {
    List<String> messages = new ArrayList<String>();
    String message;
    for(FieldValidator validator : validators) {
      message = validator.validate();
      if(message != null) {
        messages.add(message);
      }
    }

    if(messages.size() > 0) {
      eventBus.fireEvent(NotificationEvent.newBuilder().error(messages).build());
      return false;
    } else {
      return true;
    }
  }

  class SaveChangesClickHandler implements ClickHandler {

    @Override
    public void onClick(ClickEvent event) {
      if(validate()) {
        updateViewDto();
      }
    }

    private ViewDto getViewDto() {
      return viewDto;
    }

    private void updateViewDto() {
      if(getDisplay().getEntitiesToView().equals(EntitiesToView.SCRIPT)) {
        String script = getDisplay().getScript().trim();
        updateEntitiesScript(script);
      } else { // ALL
        updateEntitiesScript("");
      }
      eventBus.fireEvent(new ViewSaveRequiredEvent(getViewDto()));
    }

    private void updateEntitiesScript(String script) {
      JavaScriptViewDto jsViewDto = (JavaScriptViewDto) viewDto.getExtension(JavaScriptViewDto.ViewDtoExtensions.view);
      VariableListViewDto variableListDto = (VariableListViewDto) viewDto.getExtension(VariableListViewDto.ViewDtoExtensions.view);

      if(jsViewDto != null) {
        if(script.length() != 0) {
          jsViewDto.setWhere(script);
        } else {
          jsViewDto.clearWhere();
        }
      } else { // derived variables view
        if(script.length() != 0) {
          variableListDto.setWhere(script);
        } else {
          variableListDto.clearWhere();
        }
      }
    }
  }

  public void setViewDto(ViewDto viewDto) {
    this.viewDto = viewDto;

    TableDto tableDto = TableDto.create();
    tableDto.setDatasourceName(viewDto.getDatasourceName());
    tableDto.setName(viewDto.getName());
    scriptWidget.setTable(tableDto);

    JavaScriptViewDto jsViewDto = (JavaScriptViewDto) viewDto.getExtension(JavaScriptViewDto.ViewDtoExtensions.view);
    VariableListViewDto variableListDto = (VariableListViewDto) viewDto.getExtension(VariableListViewDto.ViewDtoExtensions.view);

    if(jsViewDto != null) {
      updateJavaScriptDisplay(jsViewDto);
    } else { // derived variables view
      updateVariableListDisplay(variableListDto);
    }
  }

  private void updateJavaScriptDisplay(JavaScriptViewDto jsViewDto) {
    if(jsViewDto.hasWhere()) {
      getDisplay().setEntitiesToView(EntitiesToView.SCRIPT);
      getDisplay().setScript(jsViewDto.getWhere());
    } else {
      getDisplay().setEntitiesToView(EntitiesToView.ALL);
      getDisplay().setScript("");
    }
  }

  private void updateVariableListDisplay(VariableListViewDto variableListDto) {
    if(variableListDto.hasWhere()) {
      getDisplay().setEntitiesToView(EntitiesToView.SCRIPT);
      getDisplay().setScript(variableListDto.getWhere());
    } else {
      getDisplay().setEntitiesToView(EntitiesToView.ALL);
      getDisplay().setScript("");
    }
  }

  class ViewConfigurationRequiredEventHandler implements ViewConfigurationRequiredEvent.Handler {

    @Override
    public void onViewConfigurationRequired(ViewConfigurationRequiredEvent event) {
      EntitiesTabPresenter.this.setViewDto(event.getView());
    }
  }

  class EntitiesToViewChangeHandler implements ChangeHandler {

    @Override
    public void onChange(ChangeEvent event) {
      if(getDisplay().getEntitiesToView() == EntitiesToView.ALL) {
        getDisplay().setScript("");
      }
      getDisplay().setScriptWidgetVisible(getDisplay().getEntitiesToView().equals(EntitiesToView.SCRIPT));
    }
  }

  class FormChangedHandler implements ChangeHandler {

    @Override
    public void onChange(ChangeEvent arg0) {
      eventBus.fireEvent(new ViewSavePendingEvent());
      getDisplay().saveChangesEnabled(true);
    }

  }

  class ViewSavedHandler implements ViewSavedEvent.Handler {

    @Override
    public void onViewSaved(ViewSavedEvent event) {
      getDisplay().saveChangesEnabled(false);
    }

  }

}