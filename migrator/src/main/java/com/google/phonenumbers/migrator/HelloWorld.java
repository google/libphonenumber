package com.google.phonenumbers.migrator;

public class HelloWorld {
    private int val;

    public HelloWorld(int val) {
        this.val = val;
    }

    @Override
    public String toString() {
        return "Hello World! The magic number is: " + val;
    }

    public static void main(String[] args) {
        HelloWorld test = new HelloWorld(5);
        System.out.println(test);
    }
}