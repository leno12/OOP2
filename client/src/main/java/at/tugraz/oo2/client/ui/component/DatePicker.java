package at.tugraz.oo2.client.ui.component;

import com.sun.javafx.scene.control.DatePickerContent;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.AccessibleRole;
import javafx.scene.control.ComboBox;
import javafx.scene.control.skin.DatePickerSkin;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.controlsfx.control.PopOver;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.TimeZone;

public class DatePicker extends ComboBox<LocalDate> {


	private LocalDate localDate;
	private javafx.scene.control.DatePicker fakeDatePicker;
	private PopOver popOver;
	private DatePickerContent content;

	public DatePicker() {
		this("DateTimePicker");
	}

	public DatePicker(String name) {
		this(FXCollections.observableArrayList(), name);
	}

	public DatePicker(ObservableList<LocalDate> items, String name) {
		super(items);
		this.setEditable(true);
		setAccessibleRole(AccessibleRole.DATE_PICKER);
		getStyleClass().add("date-picker");

		localDate = LocalDate.now();


		this.setConverter(new StringConverter<>() {
			@Override
			public String toString(LocalDate localDate) {
				if (localDate == null) {
					return null;
				}
				DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
				return localDate.format(formatter);
			}

			@Override
			public LocalDate fromString(String s) {
				try {
					DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
					return LocalDate.parse(s, formatter);
				} catch (DateTimeParseException exception) {
					try {
						return LocalDate.ofInstant(Instant.ofEpochMilli(Long.parseLong(s) * 1000), TimeZone.getDefault().toZoneId());
					} catch (NumberFormatException ex) {
						return LocalDate.now();
					}
				}
			}
		});


		fakeDatePicker = new javafx.scene.control.DatePicker();
		fakeDatePicker.valueProperty().setValue(localDate);
		fakeDatePicker.valueProperty().addListener(this::setValue);
		DatePickerSkin datePickerSkin = new DatePickerSkin(fakeDatePicker);
		fakeDatePicker.setSkin(datePickerSkin);
		content = (DatePickerContent) datePickerSkin.getPopupContent();


		GridPane gridPane = new GridPane();
		ColumnConstraints columnConstraints1 = new ColumnConstraints();
		columnConstraints1.setMinWidth(30);
		ColumnConstraints columnConstraints2 = new ColumnConstraints();
		gridPane.getColumnConstraints().addAll(columnConstraints1, columnConstraints2);
		gridPane.add(content, 0, 0, 2, 1);


		popOver = new PopOver(gridPane);
		popOver.setTitle(name);
	}

	public final String getName() {
		return popOver.getTitle();
	}

	public final void setName(String name) {
		popOver.setTitle(name);
	}

	private void setValue() {

		localDate = fakeDatePicker.getValue();
		content.goToDate(localDate, true);

		DatePicker.this.valueProperty().setValue(localDate);
	}

	public void setDate(LocalDate localDate) {
		this.localDate = localDate;

		fakeDatePicker.setValue(localDate);
		content.goToDate(localDate, true);
		DatePicker.this.valueProperty().setValue(localDate);
	}


	@Override
	public void show() {
		LocalDate current = this.getConverter().fromString(this.getEditor().getText());
		if (current == null) {
			current = LocalDate.now();
		}
		setDate(current);
		popOver.show(this);
	}

	@Override
	public void hide() {
	}

	private void setValue(Observable observable) {
		setValue();
	}

	public long getSecondsTimestamp() {
		return Timestamp.valueOf(this.getValue().atStartOfDay()).getTime() / 1000;
	}

	public LocalDate getLocalDate() {
		return localDate;
	}
}
