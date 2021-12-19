package jtg.solver;

import com.microsoft.z3.*;

public class Z3StringApi {
    private Z3StringApi(){}
    public static BoolExpr Length(Context ctx, String symbolName, int len){
        StringSymbol symbol = ctx.mkSymbol(symbolName);
        FuncDecl decl = ctx.mkConstDecl(symbol, ctx.mkStringSort());
        return ctx.parseSMTLIB2String(
                String.format("(assert (= (str.len %s) %d))", symbolName, len),
                null,
                null,
                new Symbol[]{symbol},
                new FuncDecl[]{decl}
        )[0];
    }

}
