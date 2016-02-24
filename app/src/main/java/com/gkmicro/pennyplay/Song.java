package com.gkmicro.pennyplay;

/**
 * Created by grant on 12/02/2016.
 */
public class Song {

    int Id;
    String title;
    int numplays;
    int numlikes;

    public Song (String Id, String title, String numplays, String numlikes) {
        try {
            this.Id = Integer.parseInt(Id);
        } catch (Exception e) {
            this.Id = 0;
        }
        this.title = title;
        try {
            this.numplays = Integer.parseInt(numplays);
        } catch (Exception e) {
            this.numplays = 0;
        }
        try {
            this.numlikes = Integer.parseInt(numlikes);
        } catch (Exception e) {
            this.numlikes = 0;
        }
    }

    public int getId () {
        return Id;
    }

    public String getTitle () {
        return title;
    }

    public int getNumplays(){
        return numplays;
    }

    public int getNumlikes () {
        return numlikes;
    }
}
