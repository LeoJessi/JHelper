package top.jessi.jhelper.enigma;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by Jessi on 2022/9/12
 * Email：17324719944@189.cn
 * Describe：加密算法盐
 * 随机生成字符串工具类
 */
public class Salt {
    private Salt() {
        /*私有化构造方法,阻止外部直接实例化对象*/
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    /**
     * 随机产生类型枚举
     */
    public enum TYPE {
        LETTER,                 //小写
        CAPITAL,                //大写
        NUMBER,                 //数字
        LETTER_CAPITAL,         //大写+小写
        LETTER_NUMBER,          //小写+数字
        CAPITAL_NUMBER,         //大写+数字
        LETTER_CAPITAL_NUMBER,  //大写+小写+数字
    }

    private static final String[] LOWERCASE = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n",
            "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};

    private static final String[] CAPITAL = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N",
            "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    private static final String[] NUMBER = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};

    /**
     * 静态随机数
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 获取随机组合字符串
     *
     * @param length 长度
     * @param type   类型
     * @return 随机字符串
     */
    public static String getRandomStr(int length, TYPE type) {
        List<String> temp = new ArrayList<>();        //临时存放数组
        StringBuilder code = new StringBuilder();
        /*根据传入类型添加进临时数组*/
        switch (type) {
            case LETTER -> temp.addAll(Arrays.asList(LOWERCASE));
            case CAPITAL -> temp.addAll(Arrays.asList(CAPITAL));
            case NUMBER -> temp.addAll(Arrays.asList(NUMBER));
            case LETTER_CAPITAL -> {
                temp.addAll(Arrays.asList(LOWERCASE));
                temp.addAll(Arrays.asList(CAPITAL));
            }
            case LETTER_NUMBER -> {
                temp.addAll(Arrays.asList(LOWERCASE));
                temp.addAll(Arrays.asList(NUMBER));
            }
            case CAPITAL_NUMBER -> {
                temp.addAll(Arrays.asList(CAPITAL));
                temp.addAll(Arrays.asList(NUMBER));
            }
            case LETTER_CAPITAL_NUMBER -> {
                temp.addAll(Arrays.asList(LOWERCASE));  //将小写数组添加到临时数组
                temp.addAll(Arrays.asList(CAPITAL));    //将大写数组添加到临时数组
                temp.addAll(Arrays.asList(NUMBER));     //将数字数组添加到临时数组
            }
        }
        for (int i = 0; i < length; i++) {
            code.append(temp.get(RANDOM.nextInt(temp.size())));     //将随机数添加到code中
        }
        return code.toString();
    }

    /**
     * 获取随机字符串
     *
     * @param length 长度
     * @return 随机字符串
     */
    public static String getRandomStr(int length) {
        String alphabetsInUpperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String alphabetsInLowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String allCharacters = alphabetsInLowerCase + alphabetsInUpperCase + numbers;
        StringBuilder randomString = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randomIndex = (int) (Math.random() * allCharacters.length());
            randomString.append(allCharacters.charAt(randomIndex));
        }
        return randomString.toString();
    }

    /**
     * 获取一个随机数
     *
     * @param min 最小值
     * @param max 最大值
     * @return 随机数
     */
    public static int getRandomNum(int min, int max) {
        return new Random().nextInt(max + 1 - min) + min;
    }

    /**
     * 生成一个startNum 到 endNum之间的随机数组
     *
     * @param count    总数
     * @param startNum 最小数
     * @param endNum   最大数
     * @return 返回数组
     */
    public static ArrayList<Integer> getRandomNumArr(int count, int startNum, int endNum) {
        // 如果是倒序，则将数值颠倒
        if (startNum > endNum){
            int temp = startNum;
            startNum = endNum;
            endNum = temp;
        }
        // 获取要取值的范围
        int valueRange = endNum - startNum + 1;
        // 如果要取值的总数大于取值的范围数 --> 则将总数设置为范围数，否则程序会陷入死循环
        if (count > valueRange) count = valueRange;
        Random random = new Random();
        ArrayList<Integer> arrayNum = new ArrayList<>();
        int randomValue;
        while (arrayNum.size() < count) {
            randomValue = random.nextInt(valueRange) + startNum;
            if (!arrayNum.contains(randomValue)) {
                arrayNum.add(randomValue);
            }
        }
        return arrayNum;
    }

    /**
     * 生成一个startNum 到 endNum之间的随机数组
     *
     * @param count      总数
     * @param startNum   最小数
     * @param endNum     最大数
     * @param excludeNum 需要排除的数
     * @return 返回数组
     */
    public static ArrayList<Integer> getRandomNumArr(int count, int startNum, int endNum, int excludeNum) {
        // 如果是倒序，则将数值颠倒
        if (startNum > endNum){
            int temp = startNum;
            startNum = endNum;
            endNum = temp;
        }
        // 获取要取值的范围
        int valueRange = endNum - startNum + 1;
        // 如果要取值的总数大于取值的范围数 --> 则将总数设置为范围数，否则程序会陷入死循环
        if (count > valueRange) count = valueRange;
        // 如果排除数在取值范围内且 取值范围 - 1 < 要取值的总数，则取值的总数变换为剩下可以取值的数的总个数
        if (excludeNum >= startNum && excludeNum <= endNum && ((valueRange - 1) < count)) count--;
        Random random = new Random();
        ArrayList<Integer> arrayNum = new ArrayList<>();
        int randomValue;
        while (arrayNum.size() < count) {
            randomValue = random.nextInt(valueRange) + startNum;
            if (randomValue != excludeNum && !arrayNum.contains(randomValue)) {
                arrayNum.add(randomValue);
            }
        }
        return arrayNum;
    }

    /**
     * 生成一个startNum 到 endNum之间的随机数组
     *
     * @param count         总数
     * @param startNum      最小数
     * @param endNum        最大数
     * @param excludeNumArr 需要排除的数据集合
     * @return 返回数组
     */
    public static ArrayList<Integer> getRandomNumArr(int count, int startNum, int endNum,
                                                     ArrayList<Integer> excludeNumArr) {
        // 如果是倒序，则将数值颠倒
        if (startNum > endNum){
            int temp = startNum;
            startNum = endNum;
            endNum = temp;
        }
        // 获取要取值的范围
        int valueRange = endNum - startNum + 1;
        // 如果要取值的总数大于取值的范围数 --> 则将总数设置为范围数，否则程序会陷入死循环
        if (count > valueRange) count = valueRange;
        int needExcludeSun = 0;
        for (int excludeNum : excludeNumArr) {
            // 遍历需要排除数的数组，如果有包含在取值范围内的则将个数递增记录起来
            if (excludeNum >= startNum && excludeNum <= endNum) needExcludeSun++;
        }
        // 如果取值范围 - 需要排除的个数 < 要取值的总数，则取值的总数变换为剩下可以取值的数的总个数
        if ((valueRange - needExcludeSun) < count) count = valueRange - needExcludeSun;
        Random random = new Random();
        ArrayList<Integer> arrayNum = new ArrayList<>();
        int randomValue;
        while (arrayNum.size() < count) {
            randomValue = random.nextInt(valueRange) + startNum;
            if (!excludeNumArr.contains(randomValue) && !arrayNum.contains(randomValue)) {
                arrayNum.add(randomValue);
            }
        }
        return arrayNum;
    }
}
