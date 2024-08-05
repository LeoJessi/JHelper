package top.jessi.jhelper.enigma;

/**
 * Created by Jessi on 2023/4/1 10:16
 * Email：17324719944@189.cn
 * Describe：雪花算法ID生成器
 */
public class SnowFlake {
    // 起始的时间戳，用于计算相对时间 -- 2022-12-22 00:11:56
    private static final long START_TIMESTAMP = 1671639116000L;
    // 机器ID所占位数
    private static final long WORKER_ID_BITS = 5L;
    // 数据标识ID所占位数
    private static final long DATA_CENTER_ID_BITS = 5L;
    // 支持的最大机器ID，结果是31(0b11111)
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    // 支持的最大数据标识ID，结果也是31
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);
    // 序列号所占位数
    private static final long SEQUENCE_BITS = 12L;
    // 机器ID向左移12位
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    // 数据标识ID向左移17位（12+5）
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    // 时间戳向左移22位（5+5+12）
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;
    // 生成序列号的掩码，这里为4095（0b111111111111=0xfff=4095）
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    // 工作机器ID（0~31）
    private final long WORKER_ID;
    // 数据中心ID（0~31）
    private final long DATA_CENTER_ID;
    // 每个workId下的序列号初始值
    private long mSequence = 0L;
    // 上一次生成ID的时间戳
    private long mLastTimestamp = -1L;

    /**
     * 构造函数
     *
     * @param workerID     机器标识ID（最大值不能超过MAX_MACHINE_NUM）
     * @param datCenterID 数据中心标识ID（最大值不能超过MAX_DATACENTER_NUM）
     */
    public SnowFlake(long workerID, long datCenterID) {
        // 校验机器id和数据标识id是否超出范围
        if (workerID > MAX_WORKER_ID || workerID < 0) {
            throw new IllegalArgumentException
                    (String.format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        if (datCenterID > MAX_DATA_CENTER_ID || datCenterID < 0) {
            throw new IllegalArgumentException
                    (String.format("datacenter Id can't be greater than %d or less than 0", MAX_DATA_CENTER_ID));
        }
        this.WORKER_ID = workerID;
        this.DATA_CENTER_ID = datCenterID;
    }

    /**
     * 获取下一个ID
     *
     * @return SnowflakeId
     */
    public synchronized long nextID() {
        long timestamp = timeGen();
        // 如果当前时间小于上次时间戳，说明系统时钟回退过，出现问题返回0
        if (timestamp < mLastTimestamp) {
            // ILog.e("Clock moved backwards. Refusing to generate id");
            return 0;
        }
        // 如果是同一时间生成的，则进行毫秒内序列自增
        if (mLastTimestamp == timestamp) {
            mSequence = (mSequence + 1) & SEQUENCE_MASK;
            // 毫秒内序列溢出
            if (mSequence == 0) {
                // 阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(mLastTimestamp);
            }
        }
        // 时间戳改变，毫秒内序列重置
        else {
            mSequence = 0L;
        }
        // 上次生成ID的时间截
        mLastTimestamp = timestamp;
        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT)
                | (DATA_CENTER_ID << DATA_CENTER_ID_SHIFT)
                | (WORKER_ID << WORKER_ID_SHIFT)
                | mSequence;
    }

    /**
     * 等待直到下一个毫秒
     *
     * @param lastTimestamp 最后生成ID的时间戳
     * @return 下一个毫秒的时间戳
     */
    private long tilNextMillis(final long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 生成当前时间戳
     *
     * @return 当前时间戳
     */
    private static long timeGen() {
        return System.currentTimeMillis();
    }

}
