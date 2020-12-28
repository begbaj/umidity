package com.umidity.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.umidity.Debugger;
import com.umidity.Main;
import com.umidity.api.Single;
import com.umidity.api.caller.ApiArgument;
import com.umidity.api.caller.ApiListener;
import com.umidity.Coordinates;
import com.umidity.api.response.ForecastResponse;
import com.umidity.api.response.OneCallHistoricalResponse;
import com.umidity.database.CityRecord;
import com.umidity.database.HumidityRecord;
import com.umidity.database.RecordsListener;
import com.umidity.statistics.StatsCalculator;

import java.awt.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

public class MainGui implements ApiListener, RecordsListener {
    //region JTools
    public JPanel panelMain;
    private JTextField textField_City;
    private JButton searchButton;
    private JTextField textField_ZIP;
    private JTextField textField_State;
    private JTable recordsTable;
    private JTable statisticsTable;
    private JComboBox timeStatsBox;
    private JButton settingsbutton;
    private JDatePickerImpl datePickerFrom;
    private JDatePickerImpl datePickerTo;
    private JLabel cityLabel;
    private JLabel nosuchLabel;
    private JLabel enoughLabel;
    private JCheckBox saveCityRecordsCheckBox;
    private JCheckBox setFavouriteCityCheckBox;
    private JPanel statisticPanel;
    private JPanel BigPanel;
    private JLabel label1;
    private JToolBar toolbar;
    private JButton simpleGraphButton;
    private JButton recordsGraphButton;
    private JLabel easterEggLabel;
    private JLabel timeWarningLabel;
    private JButton getLast5DaysButton;
    private ChartPanel chartPanel;
    private ChartPanel chartRecordsPanel;
    //endregion

    boolean listenerOn;
    Single realtimeResponse;
    String[] recordColumnNames;
    String[] statisticsColumnNames;
    DecimalFormatSymbols otherSymbols;
    DecimalFormat df;
    JDatePanelImpl datePanelFrom;
    JDatePanelImpl datePanelTo;
    SimpleDateFormat format;

    public MainGui(){

        //Main.dbms.loadUserSettings();

        Vector<String> recordColumnNames =new Vector<>(Arrays.asList("Min", "Max", "Avg", "Variance"));
        Vector<String> statisticsColumnNames =new Vector<>(Arrays.asList("DateTime", "Temperature", "Humidity"));

        listenerOn = true;

        otherSymbols=new DecimalFormatSymbols(Locale.getDefault());
        otherSymbols.setDecimalSeparator('.');
        df = new DecimalFormat("###.##", otherSymbols);
        format = new SimpleDateFormat("dd-MM HH:00");

        changeTheme(Main.userSettings.interfaceSettings.guiUserTheme);

        updateTable(recordsTable, null, recordColumnNames);
        updateTable(statisticsTable, null, statisticsColumnNames);

        searchButton.addActionListener((e) -> {
            try {
                nosuchLabel.setText("");
                if(!textField_City.getText().equals(""))
                    realtimeResponse = new Single(Main.caller.getByCityName(textField_City.getText(), textField_State.getText(), textField_ZIP.getText()));
                else if(!textField_ZIP.getText().equals(""))
                    realtimeResponse= new Single(Main.caller.getByZipCode(textField_ZIP.getText(), textField_State.getText()));
                else nosuchLabel.setText("You must specify the area!");
                if(!nosuchLabel.getText().equals("You must specify the area!")) {
                    Vector<Vector<String>> matrix = new Vector();
                    Vector<String> firstRow = new Vector();
                    firstRow.add(format.format(new Date(realtimeResponse.getTimestamp()*1000)));
                    firstRow.add(Double.toString(realtimeResponse.getTemp()));
                    firstRow.add(realtimeResponse.getHumidity() + "%");
                    matrix.add(firstRow);
                    ForecastResponse forecastResponse = Main.caller.getForecastByCityName(textField_City.getText(), textField_State.getText(), textField_ZIP.getText());
                    Single[] forecastRecords = forecastResponse.getSingles();

                    for (int counter = 0; counter < forecastRecords.length; ++counter) {
                        Single f_record = forecastRecords[counter];
                        Date datetime = new Date((new Timestamp(f_record.getTimestamp() * 1000)).getTime());
                        String datetimeString = format.format(datetime);
                        Vector<String> nextRow = new Vector();
                        nextRow.add(datetimeString);
                        nextRow.add(Double.toString(f_record.getTemp()));
                        nextRow.add(((float) f_record.getHumidity()) + "%");
                        matrix.add(nextRow);
                    }

                    //Controlla create Table
                    recordsTable.setModel(new DefaultTableModel(matrix, recordColumnNames));
                    recordsTable.setFillsViewportHeight(true);
                    cityLabel.setText(realtimeResponse.getCityName().toUpperCase()+ ", " + realtimeResponse.getCityCountry().toUpperCase());
                    CityRecord city = new CityRecord(realtimeResponse.getCityId(), realtimeResponse.getCityName(), realtimeResponse.getCoord());
                    HumidityRecord record = new HumidityRecord(realtimeResponse.getHumidity(), realtimeResponse.getTimestamp(), city);
                    listenerOn = false;
                    if (Main.dbms.cityisSaved(city)) {
                        saveCityRecordsCheckBox.setSelected(true);
                        //Main.dbms.addHumidity(record);
                        timeStatsBox.setSelectedIndex(0);
                        setPanelEnabled(statisticPanel, true);
                    } else {
                        saveCityRecordsCheckBox.setSelected(false);
                        updateTable(statisticsTable, null, statisticsColumnNames);
                        setPanelEnabled(statisticPanel, false);
                    }

                    setFavouriteCityCheckBox.setSelected(Main.dbms.getFavouriteCity().getId() == city.getId());
                    listenerOn = true;
                }
            } catch (FileNotFoundException ex) {
                nosuchLabel.setText("Can't find any area with such parameters");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        settingsbutton.addActionListener((e) -> {
            SettingsFrame settingsGui = new SettingsFrame();
        });

        timeStatsBox.addActionListener((e) -> {
            Calendar cal;
            Date fromDate;
            if (timeStatsBox.getSelectedIndex() == 0) {
                cal = Calendar.getInstance();
                cal.add(5, -7);
                fromDate = cal.getTime();
                createStatistic(fromDate, new Date());
            } else if (timeStatsBox.getSelectedIndex() == 1) {
                cal = Calendar.getInstance();
                cal.add(5, -30);
                fromDate = cal.getTime();
                createStatistic(fromDate, new Date());
            } else if (!datePickerFrom.getJFormattedTextField().getText().equals("") && !datePickerTo.getJFormattedTextField().getText().equals("")) {
                fromDate = (Date)datePickerFrom.getModel().getValue();
                Date toDate = (Date)datePickerTo.getModel().getValue();
                createStatistic(fromDate, toDate);
            }
        });

        datePickerFrom.addActionListener((e) -> {
            try {
                if (((Date) datePickerFrom.getModel().getValue()).before((Date) datePickerTo.getModel().getValue())) {
                    if (timeStatsBox.getSelectedIndex() == 2) {
                        timeStatsBox.setSelectedIndex(2);
                    }
                    easterEggLabel.setText("");
                    timeWarningLabel.setText("");
                } else {
                    easterEggLabel.setText("Time in Lordran is convoluted, but we ain't there.");
                    timeWarningLabel.setText("Invalid Date range!");
                }
            }catch (Exception ex){
            }
        });

        datePickerTo.addActionListener((e) -> {
            try {
                if (((Date) datePickerFrom.getModel().getValue()).before((Date) datePickerTo.getModel().getValue())) {
                    if (timeStatsBox.getSelectedIndex() == 2) {
                        timeStatsBox.setSelectedIndex(2);
                    }
                    easterEggLabel.setText("");
                    timeWarningLabel.setText("");
                } else {
                    easterEggLabel.setText("Time in Lordran is convoluted, but we ain't there.");
                    timeWarningLabel.setText("Invalid Date range!");
                }
            }catch (Exception ex){
            }

        });

        saveCityRecordsCheckBox.addActionListener((e) -> {
            if (listenerOn) {
                try {
                    CityRecord city = new CityRecord(realtimeResponse.getCityId(), realtimeResponse.getCityName(), realtimeResponse.getCoord());
                    if (saveCityRecordsCheckBox.isSelected()) {
                        boolean flag = Main.dbms.addCity(city);
                        if (flag) {
                            nosuchLabel.setText("City added!");
                            searchButton.doClick();
                            setPanelEnabled(statisticPanel, true);
                            timeStatsBox.setSelectedIndex(0);
                            updateAsynCaller();
                        } else {
                            System.out.println("SOMETHING WRONG");
                        }
                    } else if (JOptionPane.showConfirmDialog(panelMain, "Remove city and delete all its records?", "Message", 0) == 0) {
                        Main.dbms.removeCity(city);
                        nosuchLabel.setText("City removed!");
                        updateTable(statisticsTable, null, statisticsColumnNames);
                        setPanelEnabled(statisticPanel, false);
                        updateAsynCaller();
                    } else {
                        listenerOn = false;
                        saveCityRecordsCheckBox.setSelected(true);
                        listenerOn = true;
                    }
                } catch (Exception ex) {
                    nosuchLabel.setText("You have to search for an area first");
                    listenerOn = false;
                    saveCityRecordsCheckBox.setSelected(false);
                    listenerOn = true;
                }
            }

        });

        setFavouriteCityCheckBox.addActionListener((e) -> {
            if (listenerOn) {
                try {
                    if (setFavouriteCityCheckBox.isSelected()) {
                        Main.dbms.setFavouriteCity(new CityRecord(realtimeResponse.getCityId(), realtimeResponse.getCityName(), realtimeResponse.getCoord()));
                    } else {
                        Main.dbms.setFavouriteCity(new CityRecord(-1, "", new Coordinates(-1.0F, -1.0F)));
                    }
                }catch (Exception ex){
                    nosuchLabel.setText("You have to search for an area first");
                    listenerOn = false;
                    setFavouriteCityCheckBox.setSelected(false);
                    listenerOn = true;
                }
            }

        });

        simpleGraphButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultCategoryDataset dcd=new DefaultCategoryDataset();
                List<HumidityRecord> records = Main.dbms.getHumidity(realtimeResponse.getCityId());
                Calendar cal;
                Date fromDate=new Date();
                Date toDate=new Date();
                if (timeStatsBox.getSelectedIndex() == 0) {
                    cal = Calendar.getInstance();
                    cal.add(5, -7);
                    fromDate = cal.getTime();
                } else if (timeStatsBox.getSelectedIndex() == 1) {
                    cal = Calendar.getInstance();
                    cal.add(5, -30);
                    fromDate = cal.getTime();
                } else if (!datePickerFrom.getJFormattedTextField().getText().equals("") && !datePickerTo.getJFormattedTextField().getText().equals("")) {
                    fromDate = (Date)datePickerFrom.getModel().getValue();
                    toDate = (Date)datePickerTo.getModel().getValue();
                }
                double min = StatsCalculator.min(records, fromDate, toDate).getHumidity();
                double max = StatsCalculator.max(records, fromDate, toDate).getHumidity();
                double avg = StatsCalculator.avg(records, fromDate, toDate);
                dcd.setValue(min, "Humidity", "Min");
                dcd.setValue(max, "Humidity", "Max");
                dcd.setValue(avg, "Humidity", "Avg");

                JFreeChart jChart = ChartFactory.createBarChart("Humidity", null, null, dcd, PlotOrientation.VERTICAL, false, false, false);
                CategoryPlot plot=jChart.getCategoryPlot();
                plot.setRangeGridlinePaint(Color.BLACK);
                if(UIManager.getLookAndFeel().getID().equals("FlatLaf - FlatLaf Dark")) {
                    jChart.setBackgroundPaint(new Color(60, 60, 60));
                    jChart.getTitle().setPaint(Color.LIGHT_GRAY);
                    plot.setOutlinePaint(Color.GRAY);
                    plot.getRangeAxis().setTickLabelPaint(Color.LIGHT_GRAY);
                    plot.getDomainAxis().setTickLabelPaint(Color.LIGHT_GRAY);
                }
                ChartPanel chartpanel=new ChartPanel(jChart);
                chartpanel.setPreferredSize(new Dimension(400, 400));

                JFrame chartFrame = new JFrame();
                chartFrame.setTitle("Humidity Simple Graph");
                chartFrame.add(chartpanel);
                chartFrame.setVisible(true);
                chartFrame.setSize(500,400);
                chartFrame.pack();
                chartFrame.setLocationRelativeTo(null);
            }
        });

        recordsGraphButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultCategoryDataset dcd=new DefaultCategoryDataset();
                List<HumidityRecord> records = Main.dbms.getHumidity(realtimeResponse.getCityId());
                for(HumidityRecord record:records){
                    dcd.addValue(record.getHumidity(), "Humidity", new Date((new Timestamp(record.getTimestamp() * 1000)).getTime()));
                }

                JFreeChart jChart = ChartFactory.createLineChart("Humidity", null, null, dcd, PlotOrientation.VERTICAL, false, false, false);
                CategoryPlot plot=jChart.getCategoryPlot();
                plot.setRangeGridlinePaint(Color.black);
                if(UIManager.getLookAndFeel().getID().equals("FlatLaf - FlatLaf Dark")) {
                    jChart.setBackgroundPaint(new Color(60, 60, 60));
                    jChart.getTitle().setPaint(Color.LIGHT_GRAY);
                    plot.setOutlinePaint(Color.GRAY);
                    plot.getRangeAxis().setTickLabelPaint(Color.LIGHT_GRAY);
                    plot.getDomainAxis().setTickLabelPaint(Color.LIGHT_GRAY);
                }
                final LineAndShapeRenderer renderer = new LineAndShapeRenderer();
                renderer.setSeriesShapesVisible(0, true);
                plot.setRenderer(renderer);

                ChartPanel chartpanel=new ChartPanel(jChart);
                chartpanel.setPreferredSize(new Dimension(1000, 400));

                JFrame chartFrame = new JFrame();
                chartFrame.setTitle("Humidity Records Graph");
                chartFrame.add(chartpanel);
                chartFrame.setVisible(true);
                chartFrame.setSize(1000,400);
                chartFrame.pack();
                chartFrame.setLocationRelativeTo(null);
            }
        });

        getLast5DaysButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Thread thread = new Thread(){
                    public void run(){
                        System.out.println("Thread Running");
                        Calendar cal = Calendar.getInstance();
                        try {
                            List<HumidityRecord> records=new ArrayList<>();
                            CityRecord city=new CityRecord(realtimeResponse.getCityId(), realtimeResponse.getCityName(), realtimeResponse.getCoord());
                            for(int i=0; i<6; i++){
                                OneCallHistoricalResponse response= Main.caller.oneCall(realtimeResponse.getCoord().lat, realtimeResponse.getCoord().lon, cal.getTime().getTime()/1000);
                                for(var x:response.hourly){
                                    records.add(new HumidityRecord(x.humidity, x.dt, city));
                                }
                                cal.add(5, -1);
                            }
                            Main.dbms.addHumidity(records);
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                        timeStatsBox.setSelectedIndex(0);
                    }
                };

                thread.start();
            }
        });

        favouriteCityStart();
    }

    /**
     * Update content of a table.
     * @param table table to update
     * @param data data to insert
     * @param columnNames column names
     */
    public void updateTable(JTable table, Vector<Vector<String>> data, Vector<String> columnNames) {
        table.setModel(new DefaultTableModel(data, columnNames));
        table.setFillsViewportHeight(true);
        table.setDefaultEditor(Object.class, null);
    }

    public void createStatistic(Date fromDate, Date toDate) {
        try {
            simpleGraphButton.setEnabled(true);
            recordsGraphButton.setEnabled(true);
            enoughLabel.setText("");
            List<HumidityRecord> records = Main.dbms.getHumidity(realtimeResponse.getCityId());
            double min = StatsCalculator.min(records, fromDate, toDate).getHumidity();
            double max = StatsCalculator.max(records, fromDate, toDate).getHumidity();
            double avg = StatsCalculator.avg(records, fromDate, toDate);
            double variance = StatsCalculator.variance(records, fromDate, toDate);
            Vector<Vector<String>> statistics = new Vector<Vector<String>>();
            {{Double.toString(min), Double.toString(max), df.format(avg), df.format(variance)}};
            updateTable(statisticsTable, statistics, statisticsColumnNames);
        } catch (Exception ex) {
            updateTable(statisticsTable, null, statisticsColumnNames);
            enoughLabel.setText("Not enough records");
            simpleGraphButton.setEnabled(false);
            recordsGraphButton.setEnabled(false);
        }

    }

    void setPanelEnabled(JPanel panel, Boolean isEnabled) {
        panel.setEnabled(isEnabled);
        Component[] components = panel.getComponents();

        for(int i = 0; i < components.length; ++i) {
            Component component = components[i];
            if (component instanceof JPanel) {
                setPanelEnabled((JPanel)component, isEnabled);
            }

            component.setEnabled(isEnabled);
        }

    }

    private void favouriteCityStart() {
        CityRecord favouriteCity = Main.dbms.getFavouriteCity();
        if (favouriteCity.getId() != -1) {
            textField_City.setText(favouriteCity.getName());
            searchButton.doClick();
        }
        else{
            setPanelEnabled(statisticPanel, false);
        }
    }

    public void themeSetup(){

    }

    private void createUIComponents() {
        UtilDateModel modelFrom = new UtilDateModel();
        UtilDateModel modelTo = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        datePanelFrom = new JDatePanelImpl(modelFrom, p);
        datePanelTo = new JDatePanelImpl(modelTo, p);
        datePickerFrom = new JDatePickerImpl(datePanelFrom, new DateLabelFormatter());
        datePickerTo = new JDatePickerImpl(datePanelTo, new DateLabelFormatter());
        datePickerFrom.setVisible(true);
        datePickerTo.setEnabled(false);
    }

    public void updateAsynCaller(){
        Vector<Integer> ids = new Vector<>();
        for(var city:Main.dbms.getCities()){
            ids.add(city.getId());
        }
         Main.asyncCaller.setArgs((Object) ids.toArray(Integer[]::new));
    }

    public static void changeTheme(String theme){
        try {
            if(theme == "Light") UIManager.setLookAndFeel(new FlatLightLaf());
            else if (theme == "Dark") UIManager.setLookAndFeel(new FlatDarkLaf());
            else{
                Debugger.println("Tema selezionato non valido");
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize LaF");
        }finally {
            Arrays.stream(JFrame.getFrames()).forEach(x -> SwingUtilities.updateComponentTreeUI(x.getComponent(0)));
        }
    }

    @Override
    public void onReceiveCurrent(Object sender, ApiArgument arg) {
        if(realtimeResponse!=null){
            if(arg.getResponse().getCityId()==realtimeResponse.getCityId())
                Main.dbms.addHumidity(HumidityRecord.singleToHumidityRecord(arg.getResponse()));
            }
    }


    @Override
    public void onReceiveForecast(Object sender, ApiArgument arg) {

    }

    @Override
    public void onReceiveHistorical(Object sender, ApiArgument arg) {
    }

    @Override
    public void onReceive(Object sender, ApiArgument arg) {

    }

    @Override
    public void onRequestCurrent(Object sender, ApiArgument arg) {

    }

    @Override
    public void onRequestForecast(Object sender, ApiArgument arg) {

    }

    @Override
    public void onRequestHistorical(Object sender, ApiArgument arg) {

    }

    @Override
    public void onRequest(Object sender, ApiArgument arg) {

    }

    //TODO: realtimecity?????????
    @Override
    public void onChangedCities() {
        if(recordColumnNames!=null){
            if(!Main.dbms.cityisSaved(new CityRecord(realtimeResponse.getCityId(), realtimeResponse.getCityName(), realtimeResponse.getCoord())))
            {
                listenerOn=false;
                saveCityRecordsCheckBox.setSelected(false);
                setPanelEnabled(statisticPanel, false);
                nosuchLabel.setText("City removed!");
                updateTable(statisticsTable, null, statisticsColumnNames);
                listenerOn=true;
            }
        }
    }
}
