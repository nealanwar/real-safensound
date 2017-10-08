package edu.umd.realsafensoundandroid;

class Notification {
    String name;
    String[] location;
    String place;
    String photoId;
    long id;
    long time;

    Notification(String name, String[] location, String place, String photoId, long id, long time) {
        this.name = name;
        this.location = location;
        this.place = place;
        this.photoId = photoId;
        this.id = id;
        this.time = time;
    }
}
