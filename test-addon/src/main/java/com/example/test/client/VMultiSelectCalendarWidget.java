/*
 * Copyright 2000-2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.test.client;

import java.util.ArrayList;

/*
 * Copyright 2000-2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.aria.client.SelectedValue;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.BrowserInfo;
import com.vaadin.client.DateTimeService;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.ui.FocusableFlexTable;
import com.vaadin.client.ui.SubPartAware;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.shared.util.SharedUtil;

@SuppressWarnings("deprecation")
public class VMultiSelectCalendarWidget extends FocusableFlexTable
		implements KeyDownHandler, KeyPressHandler, BlurHandler, FocusHandler, SubPartAware {

	public interface SubmitListener {

		/**
		 * Called when calendar user triggers a submitting operation in calendar panel. Eg. clicking on day or hitting
		 * enter.
		 */
		void onSubmit();

		/**
		 * On eg. ESC key.
		 */
		void onCancel();
	}

	/**
	 * Blur listener that listens to blur event from the panel
	 */
	public interface FocusOutListener {
		/**
		 * @return true if the calendar panel is not used after focus moves out
		 */
		boolean onFocusOut(DomEvent<?> event);
	}

	/**
	 * FocusChangeListener is notified when the panel changes its _focused_ value.
	 */
	public interface FocusChangeListener {
		void focusChanged(Date focusedDate);
	}

	private static final String CN_FOCUSED = "focused";
	private static final String CN_TODAY = "today";
	private static final String CN_SELECTED = "selected";
	private static final String CN_OFFMONTH = "offmonth";
	private static final String CN_OUTSIDE_RANGE = "outside-range";

	private static final String STYLE_PRIMARY = "v-inline-datefield";

	/**
	 * Represents a click handler for when a user selects a value by using the mouse
	 */
	private ClickHandler dayClickHandler = new ClickHandler() {
		/*
		 * (non-Javadoc)
		 * @see com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt .event.dom.client.ClickEvent)
		 */
		@Override
		public void onClick(ClickEvent event) {

			DayWidget clickedDay = (DayWidget) event.getSource();
			if (!isDateInsideRange(clickedDay.getDate(), Resolution.DAY)) {
				return;
			}
			focusDayWidget(clickedDay);
			selectCurrentlyFocusedDay();
			onSubmit();
		}
	};

	private final Logger logger = Logger.getLogger(getClass().getName());

	private FlexTable daysTable = new FlexTable();

	private Resolution resolution = Resolution.DAY;

	private DateTimeService dateTimeService = new DateTimeService();

	private boolean showISOWeekNumbers;

	private Date displayedMonth;

	private List<Date> selectedDates = new ArrayList<>();
	private List<DayWidget> selectedDays = new ArrayList<>();

	private DayWidget focusedDay;

	private FocusOutListener focusOutListener;

	private SubmitListener submitListener;

	private FocusChangeListener focusChangeListener;

	private boolean hasFocus = false;

	private boolean initialRenderDone = false;

	public VMultiSelectCalendarWidget() {
		getElement().setId(DOM.createUniqueId());
		setStyleName(STYLE_PRIMARY + "-calendarpanel");
		Roles.getGridRole().set(getElement());

		/*
		 * Firefox auto-repeat works correctly only if we use a key press handler, other browsers handle it correctly
		 * when using a key down handler
		 */
		if (BrowserInfo.get().isGecko()) {
			addKeyPressHandler(this);
		} else {
			addKeyDownHandler(this);
		}
		addFocusHandler(this);
		addBlurHandler(this);
	}

	/**
	 * Sets the focus to given date in the current view. Used when moving in the calendar with the keyboard.
	 * 
	 * @param date
	 *            A Date representing the day of month to be focused. Must be one of the days currently visible.
	 */
	private void focusDayWidget(DayWidget day) {
		if (focusedDay != null) {
			focusedDay.removeStyleDependentName(CN_FOCUSED);
		}

		focusedDay = day;
		if (focusedDay != null) {
			focusedDay.addStyleDependentName(CN_FOCUSED);
		}
	}

	/**
	 * Sets the selection highlight to a given day in the current view
	 * 
	 * @param date
	 *            A Date representing the day of month to be selected. Must be one of the days currently visible.
	 * 
	 */
	private void toggleDate(Date date) {
		DayWidget dayToSelect = new DayWidget(date);
		for (DayWidget day : getDayWidgets()) {
			if (day.equals(dayToSelect)) {
				if (selectedDays.contains(day)) {
					day.removeStyleDependentName(CN_SELECTED);
					Roles.getGridcellRole().removeAriaSelectedState(day.getElement());
					selectedDays.remove(day);
				} else {
					day.addStyleDependentName(CN_SELECTED);
					Roles.getGridcellRole().setAriaSelectedState(day.getElement(), SelectedValue.TRUE);
					selectedDays.add(day);
				}
			}
		}
	}

	private List<DayWidget> getDayWidgets() {
		List<DayWidget> dayWidgets = new ArrayList<>();
		int rowCount = daysTable.getRowCount();
		for (int i = 0; i < rowCount; i++) {
			int cellCount = daysTable.getCellCount(i);
			for (int j = 0; j < cellCount; j++) {
				Widget widget = daysTable.getWidget(i, j);
				if (widget != null && widget instanceof DayWidget) {
					dayWidgets.add((DayWidget) widget);
				}
			}
		}
		return dayWidgets;
	}

	private DayWidget getDayWidget(Date date) {
		int rowCount = daysTable.getRowCount();
		for (int i = 0; i < rowCount; i++) {
			int cellCount = daysTable.getCellCount(i);
			for (int j = 0; j < cellCount; j++) {
				Widget widget = daysTable.getWidget(i, j);
				if (widget != null && widget instanceof DayWidget) {
					DayWidget day = (DayWidget) widget;
					if (day.getDate().equals(date)) {
						return day;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Updates year, month, day from focusedDate to value
	 */
	private void selectCurrentlyFocusedDay() {
		if (focusedDay != null && isDateInsideRange(focusedDay.getDate(), resolution)) {
			Date focusedDate = focusedDay.getDate();
			logger.info("Toggling date " + focusedDate);
			if (selectedDates.contains(focusedDate)) {
				selectedDates.remove(focusedDate);
			} else {
				selectedDates.add(focusedDate);
			}
			toggleDate(focusedDate);
		} else {
			logger.warning("Trying to select a focused date which is NULL!");
		}
	}

	public Resolution getResolution() {
		return resolution;
	}

	@Override
	public void setStyleName(String style) {
		super.setStyleName(style);
		if (initialRenderDone) {
			// Dynamic updates to the stylename needs to render the calendar to
			// update the inner element stylenames
			renderCalendar();
		}
	}

	@Override
	public void setStylePrimaryName(String style) {
		super.setStylePrimaryName(style);
		if (initialRenderDone) {
			// Dynamic updates to the stylename needs to render the calendar to
			// update the inner element stylenames
			renderCalendar();
		}
	}

	private void clearCalendarBody() {
		// Leave the cells in place but clear their contents

		// This has the side effect of ensuring that the calendar always
		// contain 7 rows.
		for (int row = 1; row < 7; row++) {
			for (int col = 0; col < 8; col++) {
				daysTable.setHTML(row, col, "&nbsp;");
			}
		}
	}

	/**
	 * Builds the top buttons and current month and year header.
	 * 
	 */
	private void buildCalendarHeader() {

		getRowFormatter().addStyleName(0, STYLE_PRIMARY + "-calendarpanel-header");

		final String monthName = getDateTimeService().getMonth(displayedMonth.getMonth());
		final int year = displayedMonth.getYear() + 1900;

		getFlexCellFormatter().setStyleName(0, 2, STYLE_PRIMARY + "-calendarpanel-month");

		setHTML(0, 2,
				"<span class=\"" + STYLE_PRIMARY + "-calendarpanel-month\">" + monthName + " " + year + "</span>");
	}

	private DateTimeService getDateTimeService() {
		return dateTimeService;
	}
	//
	//	public void setDateTimeService(DateTimeService dateTimeService) {
	//		this.dateTimeService = dateTimeService;
	//	}

	/**
	 * Returns whether ISO 8601 week numbers should be shown in the value selector or not. ISO 8601 defines that a week
	 * always starts with a Monday so the week numbers are only shown if this is the case.
	 * 
	 * @return true if week number should be shown, false otherwise
	 */
	public boolean isShowISOWeekNumbers() {
		return showISOWeekNumbers;
	}

	public void setShowISOWeekNumbers(boolean showISOWeekNumbers) {
		this.showISOWeekNumbers = showISOWeekNumbers;
	}

	/**
	 * Checks inclusively whether a date is inside a range of dates or not.
	 * 
	 * @param date
	 * @return
	 */
	private boolean isDateInsideRange(Date date, Resolution minResolution) {
		assert (date != null);

		return isAcceptedByRangeEnd(date, minResolution) && isAcceptedByRangeStart(date, minResolution);
	}

	/**
	 * Accepts dates greater than or equal to rangeStart, depending on the resolution. If the resolution is set to DAY,
	 * the range will compare on a day-basis. If the resolution is set to YEAR, only years are compared. So even if the
	 * range is set to one millisecond in next year, also next year will be included.
	 * 
	 * @param date
	 * @param minResolution
	 * @return
	 */
	private boolean isAcceptedByRangeStart(Date date, Resolution minResolution) {
		assert (date != null);

		// rangeStart == null means that we accept all values below rangeEnd
		if (rangeStart == null) {
			return true;
		}

		Date valueDuplicate = (Date) date.clone();
		Date rangeStartDuplicate = (Date) rangeStart.clone();

		if (minResolution == Resolution.YEAR) {
			return valueDuplicate.getYear() >= rangeStartDuplicate.getYear();
		}
		if (minResolution == Resolution.MONTH) {
			valueDuplicate = clearDateBelowMonth(valueDuplicate);
			rangeStartDuplicate = clearDateBelowMonth(rangeStartDuplicate);
		} else {
			valueDuplicate = clearDateBelowDay(valueDuplicate);
			rangeStartDuplicate = clearDateBelowDay(rangeStartDuplicate);
		}

		return !rangeStartDuplicate.after(valueDuplicate);
	}

	/**
	 * Accepts dates earlier than or equal to rangeStart, depending on the resolution. If the resolution is set to DAY,
	 * the range will compare on a day-basis. If the resolution is set to YEAR, only years are compared. So even if the
	 * range is set to one millisecond in next year, also next year will be included.
	 * 
	 * @param date
	 * @param minResolution
	 * @return
	 */
	private boolean isAcceptedByRangeEnd(Date date, Resolution minResolution) {
		assert (date != null);

		// rangeEnd == null means that we accept all values above rangeStart
		if (rangeEnd == null) {
			return true;
		}

		Date valueDuplicate = (Date) date.clone();
		Date rangeEndDuplicate = (Date) rangeEnd.clone();

		if (minResolution == Resolution.YEAR) {
			return valueDuplicate.getYear() <= rangeEndDuplicate.getYear();
		}
		if (minResolution == Resolution.MONTH) {
			valueDuplicate = clearDateBelowMonth(valueDuplicate);
			rangeEndDuplicate = clearDateBelowMonth(rangeEndDuplicate);
		} else {
			valueDuplicate = clearDateBelowDay(valueDuplicate);
			rangeEndDuplicate = clearDateBelowDay(rangeEndDuplicate);
		}

		return !rangeEndDuplicate.before(valueDuplicate);

	}

	private static Date clearDateBelowMonth(Date date) {
		date.setDate(1);
		return clearDateBelowDay(date);
	}

	private static Date clearDateBelowDay(Date date) {
		date.setHours(0);
		date.setMinutes(0);
		date.setSeconds(0);
		// Clearing milliseconds
		long time = date.getTime() / 1000;
		date = new Date(time * 1000);
		return date;
	}

	/**
	 * Builds the day and time selectors of the calendar.
	 */
	private void buildCalendarBody() {

		final int weekColumn = 0;
		final int firstWeekdayColumn = 1;
		final int headerRow = 0;

		setWidget(1, 0, daysTable);
		setCellPadding(0);
		setCellSpacing(0);
		getFlexCellFormatter().setColSpan(1, 0, 5);
		getFlexCellFormatter().setStyleName(1, 0, STYLE_PRIMARY + "-calendarpanel-body");

		daysTable.getFlexCellFormatter().setStyleName(headerRow, weekColumn, "v-week");
		daysTable.setHTML(headerRow, weekColumn, "<strong></strong>");
		// Hide the week column if week numbers are not to be displayed.
		daysTable.getFlexCellFormatter().setVisible(headerRow, weekColumn, isShowISOWeekNumbers());

		daysTable.getRowFormatter().setStyleName(headerRow, STYLE_PRIMARY + "-calendarpanel-weekdays");

		if (isShowISOWeekNumbers()) {
			daysTable.getFlexCellFormatter().setStyleName(headerRow, weekColumn, "v-first");
			daysTable.getFlexCellFormatter().setStyleName(headerRow, firstWeekdayColumn, "");
			daysTable.getRowFormatter().addStyleName(headerRow, STYLE_PRIMARY + "-calendarpanel-weeknumbers");
		} else {
			daysTable.getFlexCellFormatter().setStyleName(headerRow, weekColumn, "");
			daysTable.getFlexCellFormatter().setStyleName(headerRow, firstWeekdayColumn, "v-first");
		}

		daysTable.getFlexCellFormatter().setStyleName(headerRow, firstWeekdayColumn + 6, "v-last");

		// Print weekday names
		final int firstDay = getDateTimeService().getFirstDayOfWeek();
		for (int i = 0; i < 7; i++) {
			int day = i + firstDay;
			if (day > 6) {
				day = 0;
			}
			daysTable.setHTML(headerRow, firstWeekdayColumn + i,
					"<strong>" + getDateTimeService().getShortDay(day) + "</strong>");

			Roles.getColumnheaderRole().set(daysTable.getCellFormatter().getElement(headerRow, firstWeekdayColumn + i));
		}

		// Zero out hours, minutes, seconds, and milliseconds to compare dates
		// without time part
		final Date tmp = new Date();
		final Date today = new Date(tmp.getYear(), tmp.getMonth(), tmp.getDate());

		//final Date selectedDate = value == null ? null : new Date(value.getYear(), value.getMonth(), value.getDate());

		final int startWeekDay = getDateTimeService().getStartWeekDay(displayedMonth);
		final Date dateToAdd = (Date) displayedMonth.clone();
		// Start from the first day of the week that at least partially belongs
		// to the current month
		dateToAdd.setDate(1 - startWeekDay);

		// No month has more than 6 weeks so 6 is a safe maximum for rows.
		for (int weekOfMonth = 1; weekOfMonth < 7; weekOfMonth++) {
			for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {

				// Actually write the day of month
				Date dayDate = (Date) dateToAdd.clone();
				DayWidget day = new DayWidget(dayDate);

				day.setStyleName(STYLE_PRIMARY + "-calendarpanel-day");

				if (!isDateInsideRange(dayDate, Resolution.DAY)) {
					day.addStyleDependentName(CN_OUTSIDE_RANGE);
				}

				//if (dateToAdd.equals(selectedDate)) {
				if (selectedDates.contains(dateToAdd)) {
					day.addStyleDependentName(CN_SELECTED);
					Roles.getGridcellRole().setAriaSelectedState(day.getElement(), SelectedValue.TRUE);
					selectedDays.add(day);
				}
				if (dateToAdd.equals(today)) {
					day.addStyleDependentName(CN_TODAY);
				}
				if (focusedDay != null && dateToAdd.equals(focusedDay.getDate())) {
					focusedDay = day;
					if (hasFocus) {
						day.addStyleDependentName(CN_FOCUSED);
					}
				}
				if (dateToAdd.getMonth() != displayedMonth.getMonth()) {
					day.addStyleDependentName(CN_OFFMONTH);
				}

				daysTable.setWidget(weekOfMonth, firstWeekdayColumn + dayOfWeek, day);
				Roles.getGridcellRole()
						.set(daysTable.getCellFormatter().getElement(weekOfMonth, firstWeekdayColumn + dayOfWeek));

				// ISO week numbers if requested
				daysTable.getCellFormatter().setVisible(weekOfMonth, weekColumn, isShowISOWeekNumbers());

				if (isShowISOWeekNumbers()) {
					final String baseCssClass = STYLE_PRIMARY + "-calendarpanel-weeknumber";
					String weekCssClass = baseCssClass;

					int weekNumber = DateTimeService.getISOWeekNumber(dateToAdd);

					daysTable.setHTML(weekOfMonth, 0,
							"<span class=\"" + weekCssClass + "\"" + ">" + weekNumber + "</span>");
				}
				dateToAdd.setDate(dateToAdd.getDate() + 1);
			}
		}
	}

	/**
	 * Updates the calendar and text field with the selected dates.
	 */
	public void renderCalendar() {
		renderCalendar(true);
	}

	/**
	 * For internal use only. May be removed or replaced in the future.
	 * 
	 * Updates the calendar and text field with the selected dates.
	 * 
	 * @param updateDate
	 *            The value false prevents setting the selected date of the calendar based on focusedDate. That can be
	 *            used when only the resolution of the calendar is changed and no date has been selected.
	 */
	public void renderCalendar(boolean updateDate) {

		super.setStylePrimaryName(STYLE_PRIMARY + "-calendarpanel");

		if (updateDate && focusChangeListener != null) {
			focusChangeListener.focusChanged(focusedDay == null ? null : copyDate(focusedDay));
		}

		if (displayedMonth != null) {
			buildCalendarHeader();
			clearCalendarBody();
			buildCalendarBody();
		}

		initialRenderDone = true;
	}

	/**
	 * Moves the focus forward the given number of days.
	 */
	private void focusNextDay(int days) {
		if (focusedDay == null) {
			return;
		}

		Date nextDate = copyDate(focusedDay);
		nextDate.setDate(nextDate.getDate() + days);
		if (!isDateInsideRange(nextDate, resolution)) {
			logger.fine("Selected day not in allowed range, not changing focus");
			return;
		}

		DayWidget nextFocusDay = getDayWidget(nextDate);

		focusDayWidget(nextFocusDay);
	}

	private Date copyDate(DayWidget day) {
		return new Date(day.getDate().getTime());
	}

	/**
	 * Moves the focus backward the given number of days.
	 */
	private void focusPreviousDay(int days) {
		focusNextDay(-days);
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		handleKeyPress(event);
	}

	@Override
	public void onKeyPress(KeyPressEvent event) {
		handleKeyPress(event);
	}

	/**
	 * Handles the keypress from both the onKeyPress event and the onKeyDown event
	 * 
	 * @param event
	 *            The keydown/keypress event
	 */
	private void handleKeyPress(DomEvent<?> event) {
		// Check tabs
		int keycode = event.getNativeEvent().getKeyCode();
		if (keycode == KeyCodes.KEY_TAB && event.getNativeEvent().getShiftKey()) {
			if (onTabOut(event)) {
				return;
			}
		}

		// Handle the navigation
		if (handleNavigation(keycode, event.getNativeEvent().getCtrlKey() || event.getNativeEvent().getMetaKey(),
				event.getNativeEvent().getShiftKey())) {
			event.preventDefault();
		}

	}

	/**
	 * Notifies submit-listeners of a submit event
	 */
	private void onSubmit() {
		if (getSubmitListener() != null) {
			getSubmitListener().onSubmit();
		}
	}

	/**
	 * Notifies submit-listeners of a cancel event
	 */
	private void onCancel() {
		if (getSubmitListener() != null) {
			getSubmitListener().onCancel();
		}
	}

	/**
	 * Handle keyboard navigation what the resolution is set to DAY
	 * 
	 * @param keycode
	 *            The keycode to handle
	 * @param ctrl
	 *            Was the ctrl key pressed?
	 * @param shift
	 *            Was the shift key pressed?
	 * @return Return true if the key press was handled by the method, else return false.
	 */
	protected boolean handleNavigation(int keycode, boolean ctrl, boolean shift) {

		// Ctrl key is not in use
		if (ctrl) {
			return false;
		}

		/*
		 * Jumps to the next day.
		 */
		if (keycode == getForwardKey() && !shift) {
			focusNextDay(1);
			return true;

			/*
			 * Jumps to the previous day
			 */
		} else if (keycode == getBackwardKey() && !shift) {
			focusPreviousDay(1);
			return true;

			/*
			 * Jumps one week forward in the calendar
			 */
		} else if (keycode == getNextKey() && !shift) {
			focusNextDay(7);
			return true;

			/*
			 * Jumps one week back in the calendar
			 */
		} else if (keycode == getPreviousKey() && !shift) {
			focusPreviousDay(7);
			return true;

			/*
			 * Selects the value that is chosen
			 */
		} else if (keycode == getSelectKey() && !shift) {
			selectCurrentlyFocusedDay();
			onSubmit(); // submit
			return true;

		} else if (keycode == getCloseKey()) {
			onCancel();
			// TODO close event

			return true;

		} else if (keycode == getResetKey() && !shift) {
			// Restore showing value the selected value
			focusedDay = null;
			//displayedMonth = new FocusedDate(now.getYear(), now.getMonthValue(), 1);
			renderCalendar();
			return true;
		}

		return false;
	}

	/**
	 * Returns the reset key which will reset the calendar to the previous selection. By default this is backspace but
	 * it can be overriden to change the key to whatever you want.
	 * 
	 * @return
	 */
	protected int getResetKey() {
		return KeyCodes.KEY_BACKSPACE;
	}

	/**
	 * Returns the select key which selects the value. By default this is the enter key but it can be changed to
	 * whatever you like by overriding this method.
	 * 
	 * @return
	 */
	protected int getSelectKey() {
		return KeyCodes.KEY_ENTER;
	}

	/**
	 * Returns the key that closes the popup window if this is a VPopopCalendar. Else this does nothing. By default this
	 * is the Escape key but you can change the key to whatever you want by overriding this method.
	 * 
	 * @return
	 */
	protected int getCloseKey() {
		return KeyCodes.KEY_ESCAPE;
	}

	/**
	 * The key that selects the next day in the calendar. By default this is the right arrow key but by overriding this
	 * method it can be changed to whatever you like.
	 * 
	 * @return
	 */
	protected int getForwardKey() {
		return KeyCodes.KEY_RIGHT;
	}

	/**
	 * The key that selects the previous day in the calendar. By default this is the left arrow key but by overriding
	 * this method it can be changed to whatever you like.
	 * 
	 * @return
	 */
	protected int getBackwardKey() {
		return KeyCodes.KEY_LEFT;
	}

	/**
	 * The key that selects the next week in the calendar. By default this is the down arrow key but by overriding this
	 * method it can be changed to whatever you like.
	 * 
	 * @return
	 */
	protected int getNextKey() {
		return KeyCodes.KEY_DOWN;
	}

	/**
	 * The key that selects the previous week in the calendar. By default this is the up arrow key but by overriding
	 * this method it can be changed to whatever you like.
	 * 
	 * @return
	 */
	protected int getPreviousKey() {
		return KeyCodes.KEY_UP;
	}

	/**
	 * Sets the data of the Panel.
	 * 
	 * @param currentDate
	 *            The date to set
	 */
	public void setMonthDays(List<Date> dates) {

		// Check that we are not re-rendering an already active date
		if (!selectedDates.equals(dates)) {
			selectedDates = dates;

			for (Date date : selectedDates) {
				toggleDate(date);
			}
			renderCalendar();
		}
		focusDayWidget(null);
	}

	/**
	 * A widget representing a single day in the calendar panel.
	 */
	private class DayWidget extends InlineHTML {
		private final Date date;

		DayWidget(Date date) {
			super("" + date.getDate());
			this.date = date;
			addClickHandler(dayClickHandler);
		}

		public Date getDate() {
			return date;
		}

		@Override
		public boolean equals(Object obj) {
			return date.equals(((DayWidget) obj).date);
		}

		@Override
		public int hashCode() {
			return date.hashCode();
		}
	}

	public List<Date> getDates() {

		StringBuilder allDates = new StringBuilder();
		for (Date date : selectedDates) {
			allDates.append(date.toString() + ", ");
		}
		logger.info("Returning selected dates: " + allDates.toString());
		return selectedDates;
	}

	/**
	 * If true should be returned if the panel will not be used after this event.
	 * 
	 * @param event
	 * @return
	 */
	protected boolean onTabOut(DomEvent<?> event) {
		if (focusOutListener != null) {
			return focusOutListener.onFocusOut(event);
		}
		return false;
	}

	/**
	 * A focus out listener is triggered when the panel loosed focus. This can happen either after a user clicks outside
	 * the panel or tabs out.
	 * 
	 * @param listener
	 *            The listener to trigger
	 */
	public void setFocusOutListener(FocusOutListener listener) {
		focusOutListener = listener;
	}

	/**
	 * The submit listener is called when the user selects a value from the calender either by clicking the day or
	 * selects it by keyboard.
	 * 
	 * @param submitListener
	 *            The listener to trigger
	 */
	public void setSubmitListener(SubmitListener submitListener) {
		this.submitListener = submitListener;
	}

	/**
	 * The given FocusChangeListener is notified when the focused date changes by user either clicking on a new date or
	 * by using the keyboard.
	 * 
	 * @param listener
	 *            The FocusChangeListener to be notified
	 */
	public void setFocusChangeListener(FocusChangeListener listener) {
		focusChangeListener = listener;
	}

	/**
	 * Returns the submit listener that listens to selection made from the panel
	 * 
	 * @return The listener or NULL if no listener has been set
	 */
	public SubmitListener getSubmitListener() {
		return submitListener;
	}

	/*
	 * (non-Javadoc)
	 * @see com.google.gwt.event.dom.client.BlurHandler#onBlur(com.google.gwt.event .dom.client.BlurEvent)
	 */
	@Override
	public void onBlur(final BlurEvent event) {
		if (event.getSource() instanceof VMultiSelectCalendarWidget) {
			hasFocus = false;
			focusDayWidget(null);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.google.gwt.event.dom.client.FocusHandler#onFocus(com.google.gwt.event .dom.client.FocusEvent)
	 */
	@Override
	public void onFocus(FocusEvent event) {
		if (event.getSource() instanceof VMultiSelectCalendarWidget) {
			hasFocus = true;
			focusDayWidget(focusedDay);
		}
	}

	private static final String SUBPART_DAY = "day";
	private static final String SUBPART_MONTH_YEAR_HEADER = "header";

	private Date rangeStart;
	private Date rangeEnd;

	@Override
	public String getSubPartName(com.google.gwt.user.client.Element subElement) {
		if (contains(daysTable, subElement)) {
			// Day, find out which dayOfMonth and use that as the identifier
			DayWidget day = WidgetUtil.findWidget(subElement, DayWidget.class);
			if (day != null) {
				Date date = day.getDate();
				int id = date.getDate();
				// Zero or negative ids map to days of the preceding month,
				// past-the-end-of-month ids to days of the following month
				if (date.getMonth() < displayedMonth.getMonth()) {
					id -= DateTimeService.getNumberOfDaysInMonth(date);
				} else if (date.getMonth() > displayedMonth.getMonth()) {
					id += DateTimeService.getNumberOfDaysInMonth(displayedMonth);
				}
				return SUBPART_DAY + id;
			}
		} else if (getCellFormatter().getElement(0, 2).isOrHasChild(subElement)) {
			return SUBPART_MONTH_YEAR_HEADER;
		}

		return null;
	}

	/**
	 * Checks if subElement is inside the widget DOM hierarchy.
	 * 
	 * @param w
	 * @param subElement
	 * @return true if {@code w} is a parent of subElement, false otherwise.
	 */
	private boolean contains(Widget w, Element subElement) {
		if (w == null || w.getElement() == null) {
			return false;
		}

		return w.getElement().isOrHasChild(subElement);
	}

	@Override
	public com.google.gwt.user.client.Element getSubPartElement(String subPart) {
		if (subPart.startsWith(SUBPART_DAY)) {
			// Zero or negative ids map to days in the preceding month,
			// past-the-end-of-month ids to days in the following month
			int dayOfMonth = Integer.parseInt(subPart.substring(SUBPART_DAY.length()));
			Date date = new Date(displayedMonth.getYear(), displayedMonth.getMonth(), dayOfMonth);
			Iterator<Widget> iter = daysTable.iterator();
			while (iter.hasNext()) {
				Widget w = iter.next();
				if (w instanceof DayWidget) {
					DayWidget day = (DayWidget) w;
					if (day.getDate().equals(date)) {
						return day.getElement();
					}
				}
			}
		}

		if (SUBPART_MONTH_YEAR_HEADER.equals(subPart)) {
			return DOM.asOld((Element) getCellFormatter().getElement(0, 2).getChild(0));
		}
		return null;
	}

	/**
	 * Helper class to inform the screen reader that the user changed the selected date. It sets the value of a field
	 * that is outside the view, and is defined as a live area. That way the screen reader recognizes the change and
	 * reads it to the user.
	 */
	public class FocusedDate extends Date {

		public FocusedDate(int year, int month, int date) {
			super(year, month, date);
		}
	}

	public void setMonth(Date month) {
		if (!SharedUtil.equals(displayedMonth, month)) {
			rangeStart = new Date(month.getYear(), month.getMonth(), month.getDate());
			rangeEnd = new Date(month.getYear(), month.getMonth(), DateTimeService.getNumberOfDaysInMonth(month));
			displayedMonth = month;
			if (initialRenderDone) {
				renderCalendar();
			}
		}
	}
}
