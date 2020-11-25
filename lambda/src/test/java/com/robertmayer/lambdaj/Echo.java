package com.robertmayer.lambdaj;

import java.io.IOException;

public class Echo {

    public static void main(String[] args) throws IOException {
        System.out.print("This is a string with an embedded '\n' and trailing newline character\n");
        for (;;) {
            int c = System.in.read();
            if (c == -1) return;
            System.out.println(String.format("char %-3d %s", c, Character.isAlphabetic(c) ? String.valueOf((char)c) : "(not alpabetic)"));
        }
    }
}
