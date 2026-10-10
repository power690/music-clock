package com.xiaowei.music;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

public class LunarUtil {

    private final static String[] LUNAR_MONTH_NAMES = {
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    };

    private final static String[] LUNAR_DAY_NAMES = {
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    };

    private final static String[] TIAN_GAN = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};

    private final static String[] DI_ZHI = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};

    private final static String[] ZODIAC_ANIMALS = {"鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"};

    private final static int[] LUNAR_INFO = {
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5d0, 0x14573, 0x052d0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04bd7, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
        0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
        0x0d520
    };

    private final static int BASE_YEAR = 1900;

    private final static String[] SOLAR_TERMS = {
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
        "清明节", "谷雨", "立夏", "小满", "芒种", "夏至",
        "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
        "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    };

    private final static TimeZone BEIJING_TZ = TimeZone.getTimeZone("GMT+8");

    private static final Map<Integer, long[]> SOLAR_TERMS_CACHE = new LinkedHashMap<Integer, long[]>(64) {
        private static final long serialVersionUID = 1L;
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, long[]> eldest) {
            return size() > 64;
        }
    };

    public static String getLunarDate(Date date) {
        try {
            LunarDate lunarDate = solarToLunar(date);
            String cyclicalYear = getCyclicalYear(lunarDate.year);
            String animal = getZodiacAnimal(lunarDate.year);

            String monthName = LUNAR_MONTH_NAMES[lunarDate.month - 1];
            if (lunarDate.isLeap) {
                monthName = "闰" + monthName;
            }
            String dayName = LUNAR_DAY_NAMES[lunarDate.day - 1];
            return cyclicalYear + "年(" + animal + ")" + monthName + dayName;
        } catch (Exception e) {
            return "农历计算失败";
        }
    }

    public static String getFestival(Date date) {
        try {
            Calendar cal = Calendar.getInstance(BEIJING_TZ);
            cal.setTime(date);
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            int day = cal.get(Calendar.DAY_OF_MONTH);
            int week = cal.get(Calendar.DAY_OF_WEEK);
            int weekOfMonth = cal.get(Calendar.DAY_OF_WEEK_IN_MONTH);

            StringBuilder sb = new StringBuilder();

            String solar = getSolarFestival(month, day, week, weekOfMonth);
            if (!solar.isEmpty()) {
                sb.append(solar);
            }

            LunarDate lunarDate = solarToLunar(date);
            String lunar = getLunarFestival(lunarDate);
            if (!lunar.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(lunar);
            }

            String term = getSolarTermOnDate(year, month, day);
            if (!term.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(term);
            }

            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static LunarDate solarToLunar(Date date) {
        Calendar cal = Calendar.getInstance(BEIJING_TZ);
        cal.setTime(date);

        int year = cal.get(Calendar.YEAR);
        Calendar baseCal = Calendar.getInstance(BEIJING_TZ);
        baseCal.set(BASE_YEAR, 0, 31, 0, 0, 0);
        baseCal.set(Calendar.MILLISECOND, 0);

        long offset = (cal.getTimeInMillis() - baseCal.getTimeInMillis()) / 86400000L;

        int lunarYear = BASE_YEAR;
        int daysOfYear = 0;

        while (lunarYear < 2101 && offset > 0) {
            daysOfYear = getLunarYearDays(lunarYear);
            if (offset < daysOfYear) {
                break;
            }
            offset -= daysOfYear;
            lunarYear++;
        }

        int leapMonth = getLeapMonth(lunarYear);
        boolean isLeap = false;
        int lunarMonth = 1;
        int daysOfMonth = 0;
        for (int i = 1; i <= 12; i++) {
            if (leapMonth > 0 && i == (leapMonth + 1) && !isLeap) {
                i--;
                isLeap = true;
                daysOfMonth = getLeapMonthDays(lunarYear);
            } else {
                daysOfMonth = getLunarMonthDays(lunarYear, i);
            }

            if (offset < daysOfMonth) {
                lunarMonth = i;
                break;
            }

            offset -= daysOfMonth;
            if (isLeap && i == (leapMonth + 1)) {
                isLeap = false;
            }
        }

        int lunarDay = (int) offset + 1;
        LunarDate lunarDate = new LunarDate();
        lunarDate.year = lunarYear;
        lunarDate.month = lunarMonth;
        lunarDate.day = lunarDay;
        lunarDate.isLeap = isLeap;

        return lunarDate;
    }

    private static int getLunarYearDays(int year) {
        int sum = 348;
        for (int i = 0x8000; i > 0x8; i >>= 1) {
            sum += (LUNAR_INFO[year - BASE_YEAR] & i) != 0 ? 1 : 0;
        }
        return sum + getLeapMonthDays(year);
    }

    private static int getLunarMonthDays(int year, int month) {
        return (LUNAR_INFO[year - BASE_YEAR] & (0x10000 >> month)) != 0 ? 30 : 29;
    }

    private static int getLeapMonth(int year) {
        return LUNAR_INFO[year - BASE_YEAR] & 0xf;
    }

    private static int getLeapMonthDays(int year) {
        if (getLeapMonth(year) != 0) {
            return (LUNAR_INFO[year - BASE_YEAR] & 0x10000) != 0 ? 30 : 29;
        }
        return 0;
    }

    private static String getCyclicalYear(int year) {
        int num = year - 1900 + 36;
        return TIAN_GAN[num % 10] + DI_ZHI[num % 12];
    }

    private static String getZodiacAnimal(int year) {
        return ZODIAC_ANIMALS[(year - 1900 + 36) % 12];
    }

    private static String getSolarFestival(int month, int day, int week, int weekOfMonth) {
        StringBuilder sb = new StringBuilder();

        if (month == 1) {
            if (day == 1) appendFestival(sb, "元旦");
            if (day == 10) appendFestival(sb, "中国人民警察节");
            if (day == 26) appendFestival(sb, "国际海关日");
        } else if (month == 2) {
            if (day == 2) appendFestival(sb, "世界湿地日");
            if (day == 10) appendFestival(sb, "国际气象节");
            if (day == 14) appendFestival(sb, "情人节");
            if (day == 21) appendFestival(sb, "国际母语日");
            if (day == 24) appendFestival(sb, "第三世界青年日");
        } else if (month == 3) {
            if (day == 3) appendFestival(sb, "全国爱耳日");
            if (day == 5) appendFestival(sb, "学雷锋纪念日");
            if (day == 8) appendFestival(sb, "妇女节");
            if (day == 12) appendFestival(sb, "植树节");
            if (day == 14) appendFestival(sb, "国际警察日");
            if (day == 15) appendFestival(sb, "消费者权益日");
            if (day == 17) appendFestival(sb, "国际航海日");
            if (day == 21) appendFestival(sb, "世界森林日");
            if (day == 22) appendFestival(sb, "世界水日");
            if (day == 23) appendFestival(sb, "世界气象日");
            if (day == 24) appendFestival(sb, "世界防治结核病日");
        } else if (month == 4) {
            if (day == 1) appendFestival(sb, "愚人节");
            if (day == 7) appendFestival(sb, "世界卫生日");
            if (day == 22) appendFestival(sb, "世界地球日");
            if (day == 23) appendFestival(sb, "世界图书和版权日");
            if (day == 26) appendFestival(sb, "世界知识产权日");
        } else if (month == 5) {
            if (day == 1) appendFestival(sb, "劳动节");
            if (day == 4) appendFestival(sb, "青年节");
            if (day == 8) appendFestival(sb, "世界红十字日");
            if (day == 12) appendFestival(sb, "国际护士节");
            if (day == 15) appendFestival(sb, "国际家庭日");
            if (day == 17) appendFestival(sb, "世界电信日");
            if (day == 31) appendFestival(sb, "世界无烟日");
        } else if (month == 6) {
            if (day == 1) appendFestival(sb, "儿童节 世界牛奶日");
            if (day == 5) appendFestival(sb, "世界环境日");
            if (day == 6) appendFestival(sb, "全国爱眼日");
            if (day == 14) appendFestival(sb, "世界献血者日");
            if (day == 17) appendFestival(sb, "世界防治荒漠化和干旱日");
            if (day == 23) appendFestival(sb, "国际奥林匹克日");
            if (day == 25) appendFestival(sb, "全国土地日 世界海员日");
            if (day == 26) appendFestival(sb, "国际禁毒日");
        } else if (month == 7) {
            if (day == 1) appendFestival(sb, "建党节");
            if (day == 7) appendFestival(sb, "中国人民抗日战争纪念日");
            if (day == 11) appendFestival(sb, "世界人口日 中国航海日");
            if (day == 28) appendFestival(sb, "世界肝炎日");
        } else if (month == 8) {
            if (day == 1) appendFestival(sb, "建军节");
            if (day == 8) appendFestival(sb, "中国男子节(爸爸节)");
            if (day == 12) appendFestival(sb, "国际青年节");
            if (day == 19) appendFestival(sb, "中国医师节");
        } else if (month == 9) {
            if (day == 3) appendFestival(sb, "中国人民抗日战争胜利纪念日");
            if (day == 8) appendFestival(sb, "国际扫盲日");
            if (day == 10) appendFestival(sb, "教师节");
            if (day == 16) appendFestival(sb, "中国脑健康日");
            if (day == 18) appendFestival(sb, "九一八事变纪念日");
            if (day == 20) appendFestival(sb, "全国爱牙日");
            if (day == 21) appendFestival(sb, "国际和平日");
            if (day == 27) appendFestival(sb, "世界旅游日");
        } else if (month == 10) {
            if (day == 1) appendFestival(sb, "国庆节");
            if (day == 4) appendFestival(sb, "世界动物日");
            if (day == 9) appendFestival(sb, "世界邮政日");
            if (day == 10) appendFestival(sb, "世界精神卫生日");
            if (day == 13) appendFestival(sb, "中国少年先锋队诞辰日");
            if (day == 16) appendFestival(sb, "世界粮食日");
            if (day == 17) appendFestival(sb, "世界消除贫困日");
            if (day == 24) appendFestival(sb, "联合国日");
            if (day == 31) appendFestival(sb, "万圣节前夜");
        } else if (month == 11) {
            if (day == 1) appendFestival(sb, "万圣节");
            if (day == 8) appendFestival(sb, "中国记者节");
            if (day == 9) appendFestival(sb, "全国消防日");
            if (day == 11) appendFestival(sb, "光棍节");
            if (day == 17) appendFestival(sb, "国际大学生节");
            if (day == 25) appendFestival(sb, "国际消除对妇女的暴力日");
        } else if (month == 12) {
            if (day == 1) appendFestival(sb, "世界艾滋病日");
            if (day == 3) appendFestival(sb, "国际残疾人日");
            if (day == 9) appendFestival(sb, "世界足球日");
            if (day == 13) appendFestival(sb, "南京大屠杀死难者国家公祭日");
            if (day == 20) appendFestival(sb, "澳门回归纪念日");
            if (day == 21) appendFestival(sb, "国际篮球日");
            if (day == 24) appendFestival(sb, "平安夜");
            if (day == 25) appendFestival(sb, "圣诞节");
            if (day == 26) appendFestival(sb, "毛泽东诞辰纪念日");
        }

        if (month == 5 && week == Calendar.SUNDAY && weekOfMonth == 2) {
            appendFestival(sb, "母亲节");
        }
        if (month == 6 && week == Calendar.SUNDAY && weekOfMonth == 3) {
            appendFestival(sb, "父亲节");
        }
        if (month == 11 && week == Calendar.THURSDAY && weekOfMonth == 4) {
            appendFestival(sb, "感恩节");
        }

        return sb.toString();
    }

    private static String getLunarFestival(LunarDate lunarDate) {
        try {
            if (lunarDate.isLeap) {
                return "";
            }

            int lunarMonth = lunarDate.month;
            int lunarDay = lunarDate.day;
            StringBuilder sb = new StringBuilder();

            if (lunarMonth == 1) {
                if (lunarDay == 1) appendFestival(sb, "春节");
                if (lunarDay == 2) appendFestival(sb, "回娘家");
                if (lunarDay == 5) appendFestival(sb, "破五");
                if (lunarDay == 7) appendFestival(sb, "人日");
                if (lunarDay == 8) appendFestival(sb, "谷日");
                if (lunarDay == 9) appendFestival(sb, "玉皇大帝诞辰");
                if (lunarDay == 15) appendFestival(sb, "元宵节");
            } else if (lunarMonth == 2) {
                if (lunarDay == 2) appendFestival(sb, "龙抬头");
                if (lunarDay == 15) appendFestival(sb, "释迦牟尼佛涅槃");
                if (lunarDay == 19) appendFestival(sb, "观音菩萨诞辰");
                if (lunarDay == 21) appendFestival(sb, "普贤菩萨诞辰");
            } else if (lunarMonth == 3) {
                if (lunarDay == 3) appendFestival(sb, "上巳节");
                if (lunarDay == 15) appendFestival(sb, "真武大帝寿诞");
                if (lunarDay == 23) appendFestival(sb, "妈祖诞辰");
            } else if (lunarMonth == 4) {
                if (lunarDay == 4) appendFestival(sb, "文殊菩萨诞辰");
                if (lunarDay == 8) appendFestival(sb, "浴佛节");
                if (lunarDay == 14) appendFestival(sb, "吕祖诞辰");
                if (lunarDay == 15) appendFestival(sb, "钟离祖师诞辰");
                if (lunarDay == 28) appendFestival(sb, "药王菩萨诞辰");
            } else if (lunarMonth == 5) {
                if (lunarDay == 5) appendFestival(sb, "端午节");
                if (lunarDay == 13) appendFestival(sb, "关帝磨刀日");
                if (lunarDay == 20) appendFestival(sb, "丹阳祖师诞辰");
            } else if (lunarMonth == 6) {
                if (lunarDay == 6) appendFestival(sb, "天贶节");
                if (lunarDay == 19) appendFestival(sb, "观音菩萨成道日");
                if (lunarDay == 24) appendFestival(sb, "关帝诞");
            } else if (lunarMonth == 7) {
                if (lunarDay == 7) appendFestival(sb, "七夕");
                if (lunarDay == 15) appendFestival(sb, "中元节");
                if (lunarDay == 18) appendFestival(sb, "王母娘娘诞辰");
                if (lunarDay == 24) appendFestival(sb, "城隍诞辰");
            } else if (lunarMonth == 8) {
                if (lunarDay == 3) appendFestival(sb, "灶君诞");
                if (lunarDay == 15) appendFestival(sb, "中秋节");
                if (lunarDay == 18) appendFestival(sb, "酒仙节");
                if (lunarDay == 22) appendFestival(sb, "燃灯古佛诞辰");
                if (lunarDay == 27) appendFestival(sb, "孔子诞辰");
            } else if (lunarMonth == 9) {
                if (lunarDay == 9) appendFestival(sb, "重阳节");
                if (lunarDay == 17) appendFestival(sb, "金龙四大王诞");
                if (lunarDay == 19) appendFestival(sb, "观音菩萨出家日");
                if (lunarDay == 30) appendFestival(sb, "药师佛圣诞");
            } else if (lunarMonth == 10) {
                if (lunarDay == 1) appendFestival(sb, "寒衣节");
                if (lunarDay == 5) appendFestival(sb, "达摩祖师诞辰");
                if (lunarDay == 15) appendFestival(sb, "下元节");
                if (lunarDay == 27) appendFestival(sb, "紫微大帝诞辰");
            } else if (lunarMonth == 11) {
                if (lunarDay == 11) appendFestival(sb, "太乙救苦天尊圣诞");
                if (lunarDay == 17) appendFestival(sb, "阿弥陀佛圣诞");
            } else if (lunarMonth == 12) {
                if (lunarDay == 8) appendFestival(sb, "腊八节");
                if (lunarDay == 16) appendFestival(sb, "南岳大帝圣诞");
                if (lunarDay == 23) appendFestival(sb, "小年(北方)");
                if (lunarDay == 24) appendFestival(sb, "小年(南方)");
                if (lunarDay == 29) appendFestival(sb, "华严菩萨圣诞");

                int daysInMonth = getLunarMonthDays(lunarDate.year, 12);
                if (lunarDay == daysInMonth) {
                    appendFestival(sb, "除夕");
                }
            }

            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static void appendFestival(StringBuilder sb, String festival) {
        if (sb.length() > 0) sb.append(" ");
        sb.append(festival);
    }

    private static String getSolarTermOnDate(int year, int month, int day) {

        long[] terms = getSolarTermsForYear(year);
        if (matchSolarTerm(terms, year, month, day)) {
            return SOLAR_TERMS[matchSolarTermIndex(terms, year, month, day)];
        }

        if (month <= 2) {
            long[] prevTerms = getSolarTermsForYear(year - 1);
            if (matchSolarTerm(prevTerms, year, month, day)) {
                return SOLAR_TERMS[matchSolarTermIndex(prevTerms, year, month, day)];
            }
        }

        if (month == 12) {
            long[] nextTerms = getSolarTermsForYear(year + 1);
            if (matchSolarTerm(nextTerms, year, month, day)) {
                return SOLAR_TERMS[matchSolarTermIndex(nextTerms, year, month, day)];
            }
        }
        return "";
    }

    private static boolean matchSolarTerm(long[] terms, int year, int month, int day) {
        return matchSolarTermIndex(terms, year, month, day) >= 0;
    }

    private static int matchSolarTermIndex(long[] terms, int year, int month, int day) {
        Calendar cal = Calendar.getInstance(BEIJING_TZ);
        for (int i = 0; i < terms.length; i++) {
            cal.setTimeInMillis(terms[i]);
            if (cal.get(Calendar.YEAR) == year
                    && cal.get(Calendar.MONTH) + 1 == month
                    && cal.get(Calendar.DAY_OF_MONTH) == day) {
                return i;
            }
        }
        return -1;
    }

    private static long[] getSolarTermsForYear(int year) {
        synchronized (SOLAR_TERMS_CACHE) {
            long[] cached = SOLAR_TERMS_CACHE.get(year);
            if (cached != null) return cached;
        }
        long[] terms = new long[24];
        for (int i = 0; i < 24; i++) {
            terms[i] = calculateSolarTermMillis(year, i);
        }
        synchronized (SOLAR_TERMS_CACHE) {
            SOLAR_TERMS_CACHE.put(year, terms);
        }
        return terms;
    }

    private static long calculateSolarTermMillis(int year, int termIndex) {

        double targetLongitude = (termIndex * 15.0 + 285.0) % 360.0;

        Calendar guess = Calendar.getInstance(BEIJING_TZ);
        guess.set(year, Calendar.MARCH, 20, 0, 0, 0);
        guess.set(Calendar.MILLISECOND, 0);
        double jd = toJulianDay(guess.getTime()) + (termIndex - 5) * 15.218425;

        for (int iter = 0; iter < 30; iter++) {
            double lon = solarApparentLongitude(jd);
            double diff = targetLongitude - lon;

            while (diff > 180) diff -= 360;
            while (diff < -180) diff += 360;
            if (Math.abs(diff) < 1e-6) break;

            jd += diff / 0.9856474;
        }
        return fromJulianDay(jd);
    }

    private static double solarApparentLongitude(double jd) {
        double T = (jd - 2451545.0) / 36525.0;
        double n = jd - 2451545.0;

        double L0 = 280.46646 + 0.9856474 * n;
        L0 = normalizeDegree(L0);

        double M = 357.52911 + 0.98560028 * n;
        M = normalizeDegree(M);
        double Mrad = Math.toRadians(M);

        double C = (1.914602 - 0.004817 * T - 0.000014 * T * T) * Math.sin(Mrad)
                + (0.019993 - 0.000101 * T) * Math.sin(2 * Mrad)
                + 0.000289 * Math.sin(3 * Mrad);

        double theta = L0 + C;

        double omega = 125.04 - 0.052954 * n;
        double omegaRad = Math.toRadians(omega);
        double lambda = theta - 0.00569 - 0.00478 * Math.sin(omegaRad);

        return normalizeDegree(lambda);
    }

    private static double normalizeDegree(double d) {
        d = d % 360;
        if (d < 0) d += 360;
        return d;
    }

    private static double toJulianDay(Date date) {
        return date.getTime() / 86400000.0 + 2440587.5;
    }

    private static long fromJulianDay(double jd) {
        return (long) ((jd - 2440587.5) * 86400000.0);
    }

    private static class LunarDate {
        int year;
        int month;
        int day;
        boolean isLeap;
    }
}
