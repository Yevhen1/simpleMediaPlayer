package controllers;


import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    private BarChart barChart;

    @FXML
    private Button buttonPrevious;

    @FXML
    private Button buttonNext;

    @FXML
    private Button buttonPlay;

    @FXML
    private Label labelSongPlaying;

    @FXML
    private ListView<String> listViewPlayList;
    private ObservableList<String> listViewData;

    @FXML
    private ProgressBar progress = new ProgressBar();


//    play list
    private ArrayList<String> listSong=new ArrayList<String>();
    private MediaPlayer mediaPlayer;
    private int numberSong;
    private ChangeListener<Duration> progressChangeListener;
    private BarChart.Data[] series1Data = new XYChart.Data[128];
    private BarChart.Series<String, Number> series1 =  new BarChart.Series<String, Number>();

//  initialization block
    @Override
    public void initialize(URL var1, ResourceBundle var2){
        if (listSong.isEmpty()){
            listViewData = FXCollections.observableArrayList("No eny file");
        }else {
            listViewData = FXCollections.observableArrayList(listSong);
        }
        listViewPlayList.setItems(listViewData);
//      initialization barChart
        barChart.setPrefSize(400, 200);
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setBarGap(0);
        barChart.setCategoryGap(1);
        barChart.setVerticalGridLinesVisible(false);
        barChart.setHorizontalGridLinesVisible(false);
        String[] categories = new String[128];
        for (int i=0; i<series1Data.length; i++) {
            categories[i] = Integer.toString(i+1);
            series1Data[i] = new XYChart.Data<String,Number>(categories[i], 50);
            series1.getData().add(series1Data[i]);
        }
        barChart.getData().add(series1);
    }

    //play the previous song
    public void previous(){
        if (listSong.isEmpty()){
            labelSongPlaying.setText("No any file");
        }else if(numberSong >= 1) {
            mediaPlayer.stop();
            play(--numberSong);
        }
    }

    // play song
    public void play(final int songNumber){
        if (listSong.isEmpty()){
            labelSongPlaying.setText("No any file on list or dir");
        }
        numberSong=songNumber;
        mediaPlayer = new MediaPlayer(new Media(listSong.get(songNumber)));
        mediaPlayer.setOnEndOfMedia(new Runnable() {
            @Override
            public void run() {
                nextPlay();
            }
        });
        mediaPlayer.play();
        labelSongPlaying.setText( selectName( mediaPlayer.getMedia().getSource()));//write name song
        buttonPlay.setText("pause");
        progressBar(mediaPlayer);
        equalizer();
    }

    //select the song name from the url
    public String selectName( String name){
        return (name.substring(name.lastIndexOf("/") + 1 , name.lastIndexOf("."))).replace("%20" , " ");
    }

    // go to the next track after the end of the current
    public void nextPlay(){
        if (listSong.isEmpty()){
            labelSongPlaying.setText("No any file");
        }else {
            buttonPlay.setText("play");
            play(++numberSong);
        }
    }

    //play or pause track
    public void playPause(){
        if (listSong.isEmpty()){
            labelSongPlaying.setText("No any file");
        }else {
            if ("pause".equals(buttonPlay.getText())) {
                mediaPlayer.pause();
                buttonPlay.setText("play");
            }else {
                mediaPlayer.play();
                buttonPlay.setText("pause");
            }
        }
    }

    //play next song
    public void next(){
        if (listSong.isEmpty()){
        }else {
            mediaPlayer.stop();
            play(++numberSong);
        }
    }

//  changed ProgressBar value
    private void progressBar(final MediaPlayer newPlayer){
        progress.setProgress(0);
        progressChangeListener = new ChangeListener<Duration>() {
            @Override public void changed(ObservableValue<? extends Duration> observableValue, Duration oldValue, Duration newValue) {
                progress.setProgress(1.0 * newPlayer.getCurrentTime().toMillis() / newPlayer.getTotalDuration().toMillis());
            }
        };
        newPlayer.currentTimeProperty().addListener(progressChangeListener);
    }

    //add list  "Audio Files", " *.wav", "*.mp3", " *.wma", " *.aac"
    public void addDirSound(String way){
        File dir=new File(way);
        if (dir.isFile() && (dir.getName().endsWith(".mp3") || dir.getName().endsWith(".wav") || dir.getName().endsWith(".wma") || dir.getName().endsWith(".aac") )){
            listSong.add(dir.toURI().toString());
        }
        else {
            if (!dir.exists()) {
                System.out.println("Cannot find directory: " + dir);
            }
            for (File iDir : dir.listFiles()) {
                if (iDir.isFile() && (iDir.getName().endsWith(".mp3") || iDir.getName().endsWith(".wav") || iDir.getName().endsWith(".wma") || iDir.getName().endsWith(".aac"))) {
                    listSong.add(iDir.toURI().toString());
                } else if (iDir.isDirectory()) {
                    addDirSound(iDir.getAbsolutePath());
                }
            }
        }
    }

    public void listDisplay(){
        ArrayList<String> listName = new ArrayList<>();
        for (String number : listSong){
            listName.add(selectName(number));
        }
        listViewData = FXCollections.observableArrayList(listName);
        listViewPlayList.setItems(listViewData);
    }

//    equalizer
    public void equalizer(){
        mediaPlayer.setAudioSpectrumListener(new AudioSpectrumListener() {
            public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
                for (int i = 0; i < series1Data.length; i++) {
                    series1Data[i].setYValue(magnitudes[i] + 60);
                }
            }
        });
    }

//  add song and folder
    public void listViewOnDragEntered(DragEvent dragEvent){
        String string = dragEvent.getDragboard().getUrl();
        string = (string.substring(6)).replaceAll("/" , "\\\\\\\\");
        boolean empty = false;
        if (listSong.isEmpty()){
            empty = true;
        }
        addDirSound(string);
        listDisplay();
        if (!listSong.isEmpty() && empty){
        play(0);
        }
    }

//    selection of songs from the list
    public void mouseClicked (){
        if (!listSong.isEmpty()) {
            mediaPlayer.stop();
            play(listViewPlayList.getFocusModel().getFocusedIndex());
        }
    }
}
