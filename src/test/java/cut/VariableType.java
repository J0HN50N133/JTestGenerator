package cut;

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

    public boolean generateFloat(float f){
        if (f == 2.3) return true;
        return false;
    }

    public boolean generateDouble(double d){
        if (d == 2.4) return true;
        return false;
    }

    public boolean generateBool(boolean b){
        if (b) return true;
        return false;
    }

    public boolean generateString(String s){
        if(s.equals("str"))
            return true;
        if (s.length() == 1) return true;
        return false;
    }

    public boolean generateArr(int[] arr){
        if (arr.length == 1) return false;
        return true;
    }
    public boolean generateClass(A a){
        if (a.getA() > 0) return false;
        return true;
    }
}