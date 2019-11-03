package at.tugraz.oo2.client.ui.component;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.AccessibleRole;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.controlsfx.control.PopOver;

public class DurationPicker extends ComboBox<Number> {

    long duration;
    private PopOver popOver;
    private Label day;
    private Slider daySlider;
    private Label hour;
    private Slider hourSlider;
    private Label minute;
    private Slider minuteSlider;
    private Label second;
    private Slider secondSlider;

    public DurationPicker() {
        this("DurationPicker");
    }

    public DurationPicker(String name) {
        this(FXCollections.observableArrayList(), name);
    }

    public DurationPicker(ObservableList<Number> items, String name) {
        super(items);
        this.setEditable(true);
        setAccessibleRole(AccessibleRole.DATE_PICKER);
        getStyleClass().add("date-picker");


        day = new Label("00");
        day.setPadding(new Insets(0, 0, 0, 10));
        daySlider = new Slider(0, 365, 0);
        hour = new Label("00");
        hour.setPadding(new Insets(0, 0, 0, 10));
        hourSlider = new Slider(0, 23, 0);
        hourSlider.setPadding(new Insets(5, 0, 5, 0));
        minute = new Label("00");
        minute.setPadding(new Insets(0, 0, 0, 10));
        minuteSlider = new Slider(0, 59, 0);
        minuteSlider.setPadding(new Insets(0, 0, 5, 0));
        second = new Label("00");
        second.setPadding(new Insets(0, 0, 0, 10));
        secondSlider = new Slider(0, 59, 0);
        secondSlider.setPadding(new Insets(0, 0, 5, 0));
        duration = 0;

        this.setConverter(new DurationStringConverter());


        GridPane gridPane = new GridPane();
        ColumnConstraints columnConstraints1 = new ColumnConstraints();
        columnConstraints1.setMinWidth(30);
        ColumnConstraints columnConstraints2 = new ColumnConstraints();
        gridPane.getColumnConstraints().addAll(columnConstraints1, columnConstraints2);
        //gridPane.add(content, 0, 0, 2, 1);


        day.textProperty().bind(daySlider.valueProperty().asString("%.0f"));
        daySlider.valueProperty().addListener(this::setValue);

        gridPane.add(day, 0, 0);
        gridPane.add(daySlider, 1, 0);

        hour.textProperty().bind(hourSlider.valueProperty().asString("%.0f"));
        hourSlider.valueProperty().addListener(this::setValue);

        gridPane.add(hour, 0, 1);
        gridPane.add(hourSlider, 1, 1);


        minute.textProperty().bind(minuteSlider.valueProperty().asString("%.0f"));
        minuteSlider.valueProperty().addListener(this::setValue);
        gridPane.add(minute, 0, 2);
        gridPane.add(minuteSlider, 1, 2);

        second.textProperty().bind(secondSlider.valueProperty().asString("%.0f"));
        secondSlider.valueProperty().addListener(this::setValue);
        gridPane.add(second, 0, 3);
        gridPane.add(secondSlider, 1, 3);

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
        int days = (int) Math.round(daySlider.getValue());
        int hours = (int) Math.round(hourSlider.getValue());
        int minutes = (int) Math.round(minuteSlider.getValue());
        int seconds = (int) Math.round(secondSlider.getValue());

        long value = ((days * 24 + hours) * 60 + minutes) * 60 + seconds;

        if (value < 1) {
            value = 1;
            secondSlider.setValue(1);
        }

        DurationPicker.this.valueProperty().setValue(value);
        this.getEditor().setText(this.getConverter().toString(value));
    }

    public void setDuration(Number input) {
        long value = input.longValue();
        this.duration = value;
        daySlider.setValue((int) (value / 86400));
        hourSlider.setValue((int) (value / 3600) % 24);
        minuteSlider.setValue((int) (value / 60) % 60);
        secondSlider.setValue(value % 60);
        this.setValue(value);
    }


    @Override
    public void show() {
        Number current = this.getConverter().fromString(this.getEditor().getText());
        if (current == null) {
            current = 1L;
        }
        setDuration(current);
        popOver.show(this);
    }

    @Override
    public void hide() {
        setValue();
    }

    private void setValue(Observable observable) {
        setValue();

    }
}

class DurationStringConverter extends StringConverter<Number> {

    @Override
    public String toString(Number number) {
        if (number == null) {
            return "00:00:01";
        }


        long seconds = number.longValue();
        if (seconds == 0) {
            seconds = 1;
        }
        long days = (seconds / 86400);
        String dayString = "";
        if (days > 1) {
            dayString = days + " Days ";
        } else if (days > 0) {
            dayString = days + " Day ";
        }


        return String.format("%s%d:%02d:%02d", dayString, (seconds % 86400) / 3600, (seconds % 3600) / 60, (seconds % 60));
    }

    @Override
    public Number fromString(String input) {
        if (input == null) {
            return 1800;
        }
        String[] inputSplitted = input.split(" ");
        if (inputSplitted.length > 3 || inputSplitted.length == 2) {
            return 1800;
        }
        int days = 0;


        try {
            String hourMinuteSecond = inputSplitted[0];
            if (inputSplitted.length == 3) {
                days = Integer.valueOf(inputSplitted[0]);
                hourMinuteSecond = inputSplitted[2];
            }

            String[] hourMinuteSecondSplitted = hourMinuteSecond.split(":");
            if (hourMinuteSecondSplitted.length != 3) {
                return 1800;
            }

            int hour = Integer.valueOf(hourMinuteSecondSplitted[0]);
            int minute = Integer.valueOf(hourMinuteSecondSplitted[1]);
            int second = Integer.valueOf(hourMinuteSecondSplitted[2]);


            long result = ((days * 24 + hour) * 60 + minute) * 60 + second;
            return result < 1 ? 1 : result;
        } catch (NumberFormatException exception) {
            return 1800;
        }
    }
}
