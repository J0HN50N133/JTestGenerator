package cut;

public class LogicStructure {

    public int sequence(int a, int b) {
        return a + b;
    }

    public boolean soloIf(int op) {
        if (op > 0)
            return true;
        return false;
    }
//    public boolean soloIf(String s){
//        if ("Yes".equals(s)) return true;
//        return false;
//    }

    public boolean ifElse(int op) {
        if (op > 18)
            return true;
        else
            return false;
    }

    public String multipleIf(int op) {
        if (op % 15 == 0)
            return "FizzBuzz";
        else if (op % 5 == 0)
            return "Buzz";
        else if (op % 3 == 0)
            return "Fizz";
        else
            return Integer.toString(op);
    }


}
