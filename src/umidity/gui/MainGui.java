package umidity.gui;

import com.formdev.flatlaf.FlatLightLaf;
import org.jdatepicker.impl.JDatePickerImpl;
import umidity.api.ApiCaller;
import umidity.api.EMode;
import umidity.api.EUnits;
import umidity.api.response.ApiIResponse;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.UtilDateModel;
import umidity.api.response.ForecastIResponse;
import umidity.database.CityRecord;
import umidity.database.DatabaseManager;
import umidity.database.HumidityRecord;
import umidity.statistics.StatsCalculator;


public class MainGui {
    public JPanel panelMain;
    private JTextField textField_City;
    private JButton buttonApi;
    private JTextField textField_ZIP;
    private JTextField textField_State;
    private JTable recordsTable;
    private JTable forecastTable;
    private JTable statisticsTable;
    private JComboBox timeStatsBox;
    private JLabel label1;
    private JToolBar toolbar;
    private JButton settingsbutton;
    private JDatePanelImpl datePanelFrom;
    private JDatePanelImpl datePanelTo;
    private JDatePickerImpl datePickerFrom;
    private JDatePickerImpl datePickerTo;
    private JLabel cityLabel;
    private JLabel enoguhLabel;
    private JLabel nosuchLabel;
    private JPanel BigPanel;
    private JLabel enoughLabel;
    private JCheckBox saveCityRecordsCheckBox;
    private JCheckBox setFavouriteCityCheckBox;
    private JScrollPane tableScrollPane;
    private JTextArea textArea_Records;
    private JTable records;
    DatabaseManager DBMS =new DatabaseManager();
    ApiIResponse realtimeResponse;
    StatsCalculator statsCalc=new StatsCalculator();
    String[] recordColumnNames={"DateTime", "Temperature", "Humidity"}; //TODO: GESTISCI TUTTA LA QUESTIONE VECTOR
    String[] statisticsColumnNames={"Min", "Max", "Avg", "Variance"};
    public SettingsFrame settingsGui;
    boolean listenerOn=true;

    public MainGui(){
        try {
            UIManager.setLookAndFeel( new FlatLightLaf() );
        } catch( Exception ex ) {
            System.err.println( "Failed to initialize LaF" );
        }
        Vector<String> vectorRecordColumnNames=new Vector<>();
        vectorRecordColumnNames.add("DateTime");
        vectorRecordColumnNames.add("Temperature");
        vectorRecordColumnNames.add("Humidity");
//        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        SwingUtilities.updateComponentTreeUI(panelMain);
        ApiCaller caller=new ApiCaller("beb62ff92c75eefce173edf69bacd835", EMode.JSON, EUnits.Metric);
        createTable(recordsTable, null, recordColumnNames);
        createTable(statisticsTable, null, statisticsColumnNames);
        JButton settingsButton= new JButton();

        //TODO: SITUA STRANA COL SAVECITY
        buttonApi.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    nosuchLabel.setText("");
                    SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:00");
                    realtimeResponse=caller.getByCityName(textField_City.getText(), textField_State.getText(), textField_ZIP.getText());
                    Date date= new Date(new Timestamp(Long.parseLong(realtimeResponse.dt)*1000).getTime());
                    String dateString = format.format(date);
                    Vector<Vector<String>> matrix=new Vector<>();
                    Vector<String> firstRow=new Vector<>();
                    firstRow.add(dateString);
                    firstRow.add(Float.toString(realtimeResponse.main.temp));
                    firstRow.add(Float.toString(realtimeResponse.main.humidity)+"%");
                    matrix.add(firstRow);
                    ForecastIResponse forecastIResponse=caller.getForecastByCityName(textField_City.getText(), textField_State.getText(), textField_ZIP.getText());
                    for(ApiIResponse f_record:forecastIResponse.list)
                    {
                        Date datetime= new Date(new Timestamp(Long.parseLong(f_record.dt)*1000).getTime());
                        String datetimeString = format.format(datetime);
                        Vector<String> nextRow=new Vector<>();
                        nextRow.add(datetimeString);
                        nextRow.add(Float.toString(f_record.main.temp));
                        nextRow.add(Float.toString(f_record.main.humidity)+"%");
                        matrix.add(nextRow);
                    }
                    //createTable(recordsTable, records, recordColumnNames); //TODO: SISTEMALO
                    recordsTable.setModel(new DefaultTableModel(matrix, vectorRecordColumnNames));
                    recordsTable.setFillsViewportHeight(true);
                    cityLabel.setText(realtimeResponse.name.toUpperCase());
                    CityRecord city=new CityRecord(realtimeResponse.id, realtimeResponse.name, realtimeResponse.getCoord());
                    HumidityRecord record= new HumidityRecord(realtimeResponse.main.humidity, new Date(), city);
                    DBMS.addHumidity(record);
                    timeStatsBox.setSelectedIndex(0);
                    listenerOn=false;
                    if(DBMS.cityisSaved(city)){
                        saveCityRecordsCheckBox.setSelected(true);
                    }
                    listenerOn=true;
                    //caller.getForecastByCityName(textField_City.getText(), textField_State.getText(), textField_ZIP.getText());
                }
                catch (FileNotFoundException fnfException){
                    nosuchLabel.setText("Can't find any area with such parameters");
                }
                catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        settingsbutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SettingsFrame settingsGui = new SettingsFrame();
                WindowFocusListener hi = new WindowFocusListener() {
                    @Override
                    public void windowGainedFocus(WindowEvent e) {
                        SwingUtilities.updateComponentTreeUI(panelMain);
                    }

                    @Override
                    public void windowLostFocus(WindowEvent e) {
                        SwingUtilities.updateComponentTreeUI(panelMain);
                    }
                };
                settingsGui.addWindowFocusListener(hi);
            }
        });

        timeStatsBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(timeStatsBox.getSelectedIndex()==0)
                {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DATE, -7);
                    Date fromDate = cal.getTime();
                    createStatistic(fromDate, new Date());
                }
                else {
                    if (timeStatsBox.getSelectedIndex() == 1) {
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.DATE, -30);
                        Date fromDate = cal.getTime();
                        createStatistic(fromDate, new Date());
                    } else {
                        if (!datePickerFrom.getJFormattedTextField().getText().equals("") && !datePickerTo.getJFormattedTextField().getText().equals("")) {
                            Date fromDate = (Date) datePickerFrom.getModel().getValue();
                            Date toDate = (Date) datePickerTo.getModel().getValue();
                            createStatistic(fromDate, toDate);
                        }
                    }
                }
            }
        });

        datePickerFrom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(timeStatsBox.getSelectedIndex()==2){
                    timeStatsBox.setSelectedIndex(2);
                }
            }
        });

        datePickerTo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(timeStatsBox.getSelectedIndex()==2){
                    timeStatsBox.setSelectedIndex(2);
                }
            }
        });
        saveCityRecordsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (listenerOn) {
                    try {
                        CityRecord city = new CityRecord(realtimeResponse.id, realtimeResponse.name, realtimeResponse.coord);
                        if (saveCityRecordsCheckBox.isSelected()) {
                            boolean flag = DBMS.addCity(city);
                            if(flag) {
                                nosuchLabel.setText("City added!");
                            }else {
                                System.out.println("SOMETHING WRONG");
                            }
                        }else {
                            if(JOptionPane.showConfirmDialog(panelMain, "Remove city and delete all its records?","Message", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
                                DBMS.removeCity(city);
                                nosuchLabel.setText("City removed!");
                            }else{
                                listenerOn=false;
                                saveCityRecordsCheckBox.setSelected(true);
                                listenerOn=true;
                            }
                        }
                    }catch(Exception exception){
                        nosuchLabel.setText("You have to search it first");
                        exception.printStackTrace();
                        listenerOn=false;
                        saveCityRecordsCheckBox.setSelected(false);
                        listenerOn=true;
                    }
                }
            }
        });
    }

    public void createTable(JTable table, String[][] data, Object[] columnNames){
        table.setModel(new DefaultTableModel(data, columnNames));
        table.setFillsViewportHeight(true);
    }

    public void createStatistic(Date fromDate, Date toDate){
        try {
            enoughLabel.setText("");
            List<HumidityRecord> records = DBMS.getHumidity(realtimeResponse.id);
            String[][] statistics = {{
                    Double.toString((statsCalc.min(DBMS.getHumidity(realtimeResponse.id), fromDate, toDate)).getHumidity()),
                    Double.toString(statsCalc.max(DBMS.getHumidity(realtimeResponse.id), fromDate, toDate).getHumidity()),
                    Double.toString(statsCalc.avg(DBMS.getHumidity(realtimeResponse.id), fromDate, toDate)),
                    Double.toString(statsCalc.variance(DBMS.getHumidity(realtimeResponse.id), fromDate, toDate))}};
            createTable(statisticsTable, statistics, statisticsColumnNames);
        }catch (Exception e) {
            createTable(statisticsTable, null, statisticsColumnNames);
            enoughLabel.setText("Not enough records");
        }
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
        datePickerTo=new JDatePickerImpl(datePanelTo, new DateLabelFormatter());
        datePickerFrom.setVisible(true);
        datePickerTo.setEnabled(false);
    }
}