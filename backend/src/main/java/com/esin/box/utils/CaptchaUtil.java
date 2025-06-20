package com.esin.box.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class CaptchaUtil {
    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int LENGTH = 4;

    public static class CaptchaResult {
        private String code;
        private BufferedImage image;

        public CaptchaResult(String code, BufferedImage image) {
            this.code = code;
            this.image = image;
        }

        public String getCode() {
            return code;
        }

        public BufferedImage getImage() {
            return image;
        }
    }

    public static CaptchaResult generateCaptcha() {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置背景色
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 生成随机验证码
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < LENGTH; i++) {
            String rand = String.valueOf(CHARS.charAt(random.nextInt(CHARS.length())));
            code.append(rand);

            // 设置字体
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.setColor(new Color(random.nextInt(101), random.nextInt(111), random.nextInt(121)));

            // 旋转文字
            double theta = random.nextDouble() * Math.PI / 4 - Math.PI / 8;
            g.rotate(theta, 20 + i * 25, 20);
            g.drawString(rand, 20 + i * 25, 25);
            g.rotate(-theta, 20 + i * 25, 20);
        }

        // 添加干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(random.nextInt(101), random.nextInt(111), random.nextInt(121)));
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }

        g.dispose();
        return new CaptchaResult(code.toString(), image);
    }

    public static BufferedImage generateImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置背景色
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        Random random = new Random();

        for (int i = 0; i < code.length(); i++) {
            String character = String.valueOf(code.charAt(i));
            // 设置字体
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.setColor(new Color(random.nextInt(101), random.nextInt(111), random.nextInt(121)));

            // 旋转文字
            double theta = random.nextDouble() * Math.PI / 4 - Math.PI / 8;
            g.rotate(theta, 20 + i * 25, 20);
            g.drawString(character, 20 + i * 25, 25);
            g.rotate(-theta, 20 + i * 25, 20);
        }

        // 添加干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(random.nextInt(101), random.nextInt(111), random.nextInt(121)));
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }

        g.dispose();
        return image;
    }
}