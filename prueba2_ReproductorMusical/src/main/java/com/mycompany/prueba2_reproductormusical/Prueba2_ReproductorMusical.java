package com.mycompany.prueba2_reproductormusical;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import javazoom.jl.decoder.Header;



public class Prueba2_ReproductorMusical extends JFrame {

    private JLabel elapsedTime;
    private JLabel remainingTime;
    private JProgressBar progressBar;
    private JLabel songTitle;
    private JButton backButton;
    private JButton playPauseButton;
    private JButton forwardButton;
    private JButton chooseButton;
    static JList<String> songList;
    private JScrollPane scrollPane;
    private JLabel genre;
    private JLabel image;
    

    private AdvancedPlayer player;
    private Thread playerThread;
    private Timer timer;
    private int elapsedTimeInSeconds = 0;
    private int totalDurationInSeconds = 0;
    private String currentFilePath;

    private boolean isPlaying = false;
    private boolean isPaused = false;

    public Prueba2_ReproductorMusical() {
        setTitle("Reproductor de Musica");
        setSize(530, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initComponents();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initComponents() {
        SwingUtilities.invokeLater(() -> {
            progressBar =new JProgressBar(0, 100);
            progressBar.setBounds(20, 10, 470, 10);

            elapsedTime =new JLabel("0:00");
            elapsedTime.setBounds(50, 25, 50, 10);

            remainingTime =new JLabel("0:00");
            remainingTime.setBounds(430, 25, 50, 10);

            songTitle = new JLabel("Now Playing: ");
            songTitle.setBounds(20, 40, 470, 25);

            genre=new JLabel("Tipo: ");
            genre.setBounds(20,100,34,25);
            
            image=new JLabel("Image here");
            image.setBounds(230,70,25,25);
            
            backButton =new JButton("<<");
            backButton.setBounds(30, 70, 60, 25);

            playPauseButton =new JButton(">");
            playPauseButton.setBounds(90, 70, 80, 25);
            playPauseButton.addActionListener(e -> {
                 if (isPlaying && !isPaused) {
                    pausePlayer();
                 } else if (isPlaying && isPaused) {
                    resumePlayer();
                    }
            });

            forwardButton =new JButton(">>");
            forwardButton.setBounds(170, 70, 60, 25);

            chooseButton =new JButton("Add ...");
            chooseButton.setBounds(360, 65, 90, 40);

            songList =new JList<>();
            songList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            DefaultListModel<String> listModel =new DefaultListModel<>();
            songList.setModel(listModel);
            scrollPane = new JScrollPane(songList);
            scrollPane.setBounds(20, 120, 470, 300);

            setLayout(null);

            add(elapsedTime);
            add(remainingTime);
            add(progressBar);
            add(songTitle);
            add(backButton);
            add(playPauseButton);
            add(forwardButton);
            add(chooseButton);
            add(genre);
            add(scrollPane);

            chooseButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new addWIN().setVisible(true);
                }
            });

            songList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    stopPlayer();
                    playSelectedMp3();
                }
            });

            createPlaylistFile();

            loadSavedSongs();
        });
    }    

    private void createPlaylistFile() {
        File configFile =new File("playlist.txt");

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void playMp3(String filePath) {
        stopPlayer();

    try {
        currentFilePath =filePath;
        FileInputStream fileInputStream = new FileInputStream(filePath);
        Bitstream bitstream = new Bitstream(fileInputStream);
        isPlaying = true;

        Header header = bitstream.readFrame();
        totalDurationInSeconds = (int) header.total_ms((int) fileInputStream.getChannel().size()) / 1000;

        SwingUtilities.invokeLater(() -> {
            elapsedTime.setText("0:00");
            updateRemainingTimeLabel();
        });

        bitstream.close();
        fileInputStream.close();

        fileInputStream =new FileInputStream(filePath);
        player = new AdvancedPlayer(fileInputStream);
        player.setPlayBackListener(new PlaybackListener() {
            @Override
            public void playbackFinished(PlaybackEvent evt) {
                stopPlayer();
            }
        });

        SwingUtilities.invokeLater(() -> songTitle.setText("Now Playing: "+ getSelectedSongName()));

        playerThread = new Thread(() -> {
            try {
                player.play();
            } catch (JavaLayerException e) {
                e.printStackTrace();
            }
        });
        playerThread.start();

        startTimer();

    } catch (IOException | JavaLayerException e) {
        e.printStackTrace();
    }
    }
    
    private void updateRemainingTimeLabel(){
        if (totalDurationInSeconds > elapsedTimeInSeconds) {
            int remainingSeconds = totalDurationInSeconds - elapsedTimeInSeconds;
            int minutes =remainingSeconds / 60;
            int seconds =remainingSeconds % 60;

            SwingUtilities.invokeLater(() -> {
                remainingTime.setText(String.format("%d:%02d", minutes, seconds));
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                remainingTime.setText("0:00");
            });
        }
    }

    private void startTimer() {
        timer = new Timer(1000, e -> {
            elapsedTimeInSeconds++;

            int minutes = elapsedTimeInSeconds / 60;
            int seconds = elapsedTimeInSeconds % 60;

            SwingUtilities.invokeLater(() -> {
                elapsedTime.setText(String.format("%d:%02d", minutes, seconds));
            });
            updateRemainingTimeLabel();
        });

        timer.start();
    }

    private void stopPlayer() {
        if (player !=null) {
            player.close();
            playerThread.interrupt();
            stopTimer();
        }
    }

    private void stopTimer() {
        if (timer !=null) {
            timer.stop();
            elapsedTimeInSeconds =0;
        }
    }

    private String getSelectedSongName() {
        int selectedIndex = songList.getSelectedIndex();
        if (selectedIndex !=-1) {
            return songList.getModel().getElementAt(selectedIndex);
        }
        return "";
    }

    private void playSelectedMp3() {
        int selectedIndex =songList.getSelectedIndex();
    if (selectedIndex != -1) {
        String selectedFileName =songList.getModel().getElementAt(selectedIndex);
        currentFilePath = "songs/"+selectedFileName;
        System.out.println("File path: " +currentFilePath);
        playMp3(currentFilePath);
    }
    }

private void loadSavedSongs() {
    try (BufferedReader reader = new BufferedReader(new FileReader("playlist.txt"))) {
        String line;
        DefaultListModel<String> listModel =(DefaultListModel<String>) songList.getModel();
        while ((line = reader.readLine()) !=null) {
            String[] parts = line.split(";;");
            if (parts.length>=3) {
                String title =getTitle(parts[0]);
                listModel.addElement(title);
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}

private String getTitle(String fullTitle) {
    return fullTitle.split(";;")[0];
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Prueba2_ReproductorMusical();
        });
    }
    
    private void pausePlayer() {
    if (player !=null) {
        player.close();
        playerThread.interrupt();
        stopTimer();
        isPaused = true;
        playPauseButton.setText(">");
    }
}

private void resumePlayer() {
     if (player != null && currentFilePath !=null) {
        try {
            FileInputStream fileInputStream =new FileInputStream(currentFilePath);
            Bitstream bitstream =new Bitstream(fileInputStream);

            Header header = bitstream.readFrame();
            totalDurationInSeconds = (int) header.total_ms((int) fileInputStream.getChannel().size()) / 1000;

            player.close();
            playerThread.interrupt();
            stopTimer();

            player =new AdvancedPlayer(fileInputStream);
            player.setPlayBackListener(new PlaybackListener() {
                @Override
                public void playbackFinished(PlaybackEvent evt) {
                    stopPlayer();
                }
            });

            SwingUtilities.invokeLater(() -> songTitle.setText("Now Playing: " + getSelectedSongName()));

            playerThread =new Thread(() -> {
                try {
                    player.play();
                } catch (JavaLayerException ex) {
                    ex.printStackTrace();
                }
            });
            playerThread.start();

            startTimer();

            isPaused = false;
            playPauseButton.setText("||");

        } catch (IOException | JavaLayerException e) {
            e.printStackTrace();
        }
    }
}
    
}
