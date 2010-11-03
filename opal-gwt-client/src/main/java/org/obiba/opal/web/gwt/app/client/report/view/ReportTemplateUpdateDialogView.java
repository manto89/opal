/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.gwt.app.client.report.view;

import java.util.ArrayList;
import java.util.List;

import org.obiba.opal.web.gwt.app.client.report.presenter.ReportTemplateUpdateDialogPresenter.Display;
import org.obiba.opal.web.gwt.app.client.widgets.presenter.FileSelectionPresenter;
import org.obiba.opal.web.gwt.app.client.widgets.presenter.ItemSelectorPresenter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class ReportTemplateUpdateDialogView extends Composite implements Display {

  @UiTemplate("ReportTemplateUpdateDialogView.ui.xml")
  interface ReportTemplateUpdateDialogUiBinder extends UiBinder<DialogBox, ReportTemplateUpdateDialogView> {
  }

  private static ReportTemplateUpdateDialogUiBinder uiBinder = GWT.create(ReportTemplateUpdateDialogUiBinder.class);

  @UiField
  DialogBox dialog;

  @UiField
  Button updateReportTemplateButton;

  @UiField
  Button cancelButton;

  @UiField
  TextBox reportTemplateName;

  @UiField
  ListBox format;

  @UiField
  TextBox schedule;

  @UiField
  SimplePanel designFilePanel;

  @UiField
  CheckBox isScheduled;

  @UiField
  SimplePanel notificationEmailsPanel;

  @UiField
  SimplePanel reportParametersPanel;

  private ItemSelectorPresenter.Display emailsSelector;

  private ItemSelectorPresenter.Display parametersSelector;

  private FileSelectionPresenter.Display fileSelection;

  public ReportTemplateUpdateDialogView() {
    initWidget(uiBinder.createAndBindUi(this));
    uiBinder.createAndBindUi(this);
    dialog.setGlassEnabled(false);
    dialog.hide();
  }

  @Override
  public Widget asWidget() {
    return this;
  }

  @Override
  public void startProcessing() {
  }

  @Override
  public void stopProcessing() {
  }

  @Override
  public void showDialog() {
    dialog.center();
    dialog.show();
    reportTemplateName.setFocus(true);
  }

  @Override
  public void hideDialog() {
    dialog.hide();
  }

  @Override
  public Button getCancelButton() {
    return cancelButton;
  }

  @Override
  public Button getUpdateReportTemplateButton() {
    return updateReportTemplateButton;
  }

  @Override
  public HasCloseHandlers getDialog() {
    return dialog;
  }

  @Override
  public HasText getName() {
    return reportTemplateName;
  }

  @Override
  public String getDesignFile() {
    return fileSelection.getFile();
  }

  @Override
  public List<String> getNotificationEmails() {
    return emailsSelector.getItems();
  }

  @Override
  public String getFormat() {
    return format.getItemText(format.getSelectedIndex());
  }

  @Override
  public HasText getShedule() {
    return schedule;
  }

  @Override
  public void setDesignFileWidgetDisplay(FileSelectionPresenter.Display display) {
    designFilePanel.setWidget(display.asWidget());
    fileSelection = display;
    fileSelection.setEnabled(true);
    fileSelection.setFieldWidth("20em");
  }

  @Override
  public void setNotificationEmailsWidgetDisplay(ItemSelectorPresenter.Display display) {
    notificationEmailsPanel.setWidget(display.asWidget());
    emailsSelector = display;
  }

  @Override
  public HandlerRegistration addEnableScheduleClickHandler() {
    return isScheduled.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent event) {
        boolean scheduleRequired = ((CheckBox) event.getSource()).getValue();
        schedule.setEnabled(scheduleRequired);
        if(!scheduleRequired) {
          schedule.setText(null);
        }
      }
    });
  }

  @Override
  public void setReportParametersWidgetDisplay(ItemSelectorPresenter.Display display) {
    reportParametersPanel.setWidget(display.asWidget());
    parametersSelector = display;
  }

  @Override
  public List<String> getReportParameters() {
    return parametersSelector.getItems();
  }

  @Override
  public void setName(String name) {
    reportTemplateName.setText(name != null ? name : "");
  }

  @Override
  public void setDesignFile(String designFile) {
    fileSelection.setFile(designFile != null ? designFile : "");
  }

  @Override
  public void setFormat(String format) {
    int itemCount = this.format.getItemCount();
    String item;
    for(int i = 0; i < itemCount; i++) {
      item = this.format.getItemText(i);
      if(item.equals(format)) {
        this.format.setSelectedIndex(i);
        break;
      }
    }
  }

  @Override
  public void setSchedule(String schedule) {
    this.schedule.setText(schedule);
    if(!schedule.equals("")) {
      isScheduled.setValue(true);
      this.schedule.setEnabled(true);
    } else {
      isScheduled.setValue(false);
      this.schedule.setEnabled(false);
    }
  }

  @Override
  public void setNotificationEmails(List<String> emails) {
    emailsSelector.clear();
    for(String email : emails) {
      emailsSelector.addItem(email);
    }
  }

  @Override
  public void setReportParameters(List<String> params) {
    parametersSelector.clear();
    for(String param : params) {
      parametersSelector.addItem(param);
    }
  }

  @Override
  public void setEnabledReportTemplateName(boolean enabled) {
    reportTemplateName.setEnabled(enabled);

  }

  @Override
  public void clear() {
    setSchedule("");
    setName("");
    setDesignFile("");
    setReportParameters(new ArrayList<String>());
    setNotificationEmails(new ArrayList<String>());
    setFormat("PDF");
  }

  @Override
  public HasValue<Boolean> isScheduled() {
    return isScheduled;
  }

}
