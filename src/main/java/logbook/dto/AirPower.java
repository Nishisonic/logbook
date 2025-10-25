package logbook.dto;

import logbook.config.AppConfig;

public class AirPower {

    private int min;
    private int max;

    public AirPower(int mid) {
        this(mid, mid);
    }

    public AirPower(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public void add(AirPower air) {
        this.add(air.min, air.max);
    }

    public void add(int min, int max) {
        this.addMin(min);
        this.addMax(max);
    }

    public void addMin(int min) {
        this.min += min;
    }

    public void addMax(int max) {
        this.max += max;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    @Override
    public String toString() {
        int method = AppConfig.get().getSeikuMethod();
        switch (method) {
            case 0: // 艦載機素の制空値
                return Integer.toString(this.max);
            case 1: // 制空推定値範囲
            case 2: // 制空推定値範囲(艦載機素の制空値 + 熟練度ボーナス推定値)
                return this.min == this.max ? Integer.toString(this.max) : this.min + "-" + this.max;
            case 3: // 制空推定値中央
            case 4: // 制空推定値中央(艦載機素の制空値 + 熟練度ボーナス推定値)
                return Integer.toString((this.min + this.max) / 2);
        }
        return Integer.toString(this.max);
    }
}
