package com.gkmicro.pennyplay;

/**
 * Created by grant on 08/02/2016.
 */
public class Constants {
    public interface ACTION {
        public static String MAIN_ACTION = "com.gkmicro.pennyplay.action.main";
        public static String PREV_ACTION = "com.gkmicro.pennyplay.action.prev";
        public static String PLAY_ACTION = "com.gkmicro.pennyplay.action.play";
        public static String NEXT_ACTION = "com.gkmicro.pennyplay.action.next";
        public static String STARTFOREGROUND_ACTION = "com.gkmicro.pennyplay.action.startforeground";
        public static String STOPFOREGROUND_ACTION = "com.gkmicro.pennyplay.action.stopforeground";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }
}
