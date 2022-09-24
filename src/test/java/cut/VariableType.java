package cut;

import java.math.BigDecimal;

class A {
    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    int a;
}
public class VariableType {

    public boolean generateInt(int i){
        if (i >= 0) return true;
        return false;
    }

    public boolean generateShort(short s){
        if (s >= 10) return true;
        return false;
    }

    public boolean generateLong(long l){
        if (l >= 10) return true;
        return false;
    }

    public boolean generateChar(char c){
        if (c == 'a') return true;
        return false;
    }

    public boolean generateFloat(float f1, float f2, float f4, float f3, float f5){
        if (f1 == 2.3) return true;
        if (f2 >= 2.3) return true;
        if (f3 <= 2.3) return true;
        if (f4 > 2.3) return true;
        if (f5 < 2.3) return true;
        return false;
    }

    public boolean generateDouble(double d){
        if (d == 2.4) return true;
        return false;
    }

    public int generateBool(boolean b){
        int result = 0;
        if(b) result = 0;
        if(b == true) result = 1;
        if(!b) result = 2;
        if(b == false) result = 3;
        if(b != true) result = 4;
        if(b == true || b == false) result = 5;
        if(b == true && b == false) result = 6;
        if(b == true && b != false) result = 7;
        return result;
    }

    public boolean generateString(String s){
        if (s.length() == 1) return true;
        if(s.equals("str"))
            return true;
        if(s.length() > 5 && s.substring(1,4).equals("str"))
            return true;
        if (s.contains("abc"))
            return true;
        if (s.indexOf("cba") == 2)
            return true;
        if (s.indexOf("cba", 3) == 4)
            return true;
        if (s.startsWith("bar"))
            return true;
        if (s.endsWith("foo"))
            return true;
        return false;
    }

    public boolean generateArr(int[] arr, int j){
        if (arr.length <= j) return false;
        int i = arr[j];
        if (i >= 0) return true;
        return true;
    }
    public boolean generateClass(A a){
        if (a.getA() > 0) return false;
        return true;
    }
}