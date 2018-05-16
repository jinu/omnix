package com.omnix.manager.statistics;


public class DocumentsStat {
    private String key = "";
    
    private long count;
    private double min = Long.MAX_VALUE;
    private double max = Long.MIN_VALUE;
    private double sum;
    private double pow2sum;
    
    private double avg;
    private double stdev;
    
    /** collector에서만 사용함. */
    private boolean flag;
    
    /** 생성당시의 slot 번호 */
    private int slot;
    
    /** CSV serialize 의 구분자 */
    public static final String DIVIDE_FIELD = ",";
    
    public DocumentsStat() {
    }
    
    public DocumentsStat(int slot) {
        this.slot = slot;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public long getCount() {
        return count;
    }
    
    public void setCount(long count) {
        this.count = count;
    }
    
    public void addCount(long count) {
        this.count += count;
    }
    
    public double getMin() {
        if (this.count < 1) {
            return 0;
        }
        return min;
    }
    
    public void setMin(double min) {
        this.min = min;
    }
    
    public double getMax() {
        if (this.count < 1) {
            return 0;
        }
        return max;
    }
    
    public void setMax(double max) {
        this.max = max;
    }
    
    public double getSum() {
        return sum;
    }
    
    public void setSum(double sum) {
        this.sum = sum;
    }
    
    public void addSum(double sum) {
        this.sum += sum;
    }
    
    public double getPow2sum() {
        return pow2sum;
    }
    
    public void setPow2sum(double pow2sum) {
        this.pow2sum = pow2sum;
    }
    
    public void addPow2sum(double pow2sum) {
        this.pow2sum += pow2sum;
    }
    
    public int getSlot() {
        return slot;
    }
    
    public void setSlot(int slot) {
        this.slot = slot;
    }
    
    public double getStdev() {
        return stdev;
    }
    
    public void setStdev(double stdev) {
        this.stdev = stdev;
    }
    
    public double getAvg() {
        return avg;
    }
    
    public void setAvg(double avg) {
        this.avg = avg;
    }
    
    public boolean isFlag() {
        return flag;
    }
    
    public void setFlag(boolean flag) {
        this.flag = flag;
    }
    
    public void makeAvg() {
        if (this.count > 0) {
            this.avg = (this.sum / this.count);
        }
    }
    
    public void makeStdev() {
        if (this.pow2sum > 0) {
            double pow2avg = this.pow2sum / this.count;
            this.stdev = Math.sqrt(pow2avg - Math.pow(this.avg, 2));
        }
    }
    
    @Override
    public String toString() {
        return "DocumentsStat [key=" + key + ", count=" + count + ", min=" + min + ", max=" + max + ", sum=" + sum + ", pow2sum=" + pow2sum + ", avg=" + avg + ", stdev=" + stdev + ", slot=" + slot + "]";
    }
    
    public String toCsvString() {
        return DocumentStatUtils.makeStringFromDocument(this);
    }
}
