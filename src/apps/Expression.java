package apps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;

import structures.Stack;

public class Expression {

	/**
	 * Expression to be evaluated
	 */
	String expr;                
    
	/**
	 * Scalar symbols in the expression 
	 */
	ArrayList<ScalarSymbol> scalars;   
	
	/**
	 * Array symbols in the expression
	 */
	ArrayList<ArraySymbol> arrays;
    
	/**
	 * Positions of opening brackets
	 */
	ArrayList<Integer> openingBracketIndex; 
    
	/**
	 * Positions of closing brackets
	 */
	ArrayList<Integer> closingBracketIndex; 

    /**
     * String containing all delimiters (characters other than variables and constants), 
     * to be used with StringTokenizer
     */
    public static final String delims = " \t*+-/()[]";
    private int bracketCount = 0;
    
    /**
     * Initializes this Expression object with an input expression. Sets all other
     * fields to null.
     * 
     * @param expr Expression
     */
    public Expression(String expr) {
        this.expr = expr;
        scalars = null;
        arrays = null;
        openingBracketIndex = null;
        closingBracketIndex = null;
    }

    /**
     * Matches parentheses and square brackets. Populates the openingBracketIndex and
     * closingBracketIndex array lists in such a way that closingBracketIndex[i] is
     * the position of the bracket in the expression that closes an opening bracket
     * at position openingBracketIndex[i]. For example, if the expression is:
     * <pre>
     *    (a+(b-c))*(d+A[4])
     * </pre>
     * then the method would return true, and the array lists would be set to:
     * <pre>
     *    openingBracketIndex: [0 3 10 14]
     *    closingBracketIndex: [8 7 17 16]
     * </pe>
     * 
     * @return True if brackets are matched correctly, false if not
     */
    public boolean isLegallyMatched() 
    {
    	int length = expr.length();
        int num = 0;
        int count = 0;
        Stack <Character> openParenBrack = new Stack <Character>();
        Stack <Integer> location = new Stack <Integer>();
        openingBracketIndex = new ArrayList <Integer>();
        closingBracketIndex = new ArrayList <Integer>();
       
        for (int a = 0; a < length; a++)
        {
                if(expr.charAt(a) == ')' || expr.charAt(a) == ']')
                        num++;
        }
        for (int b = 0; b <= num; b++)
                closingBracketIndex.add(0);
        for (int i = 0; i < length; i++)
        {
	        if (expr.charAt(i) == '(' || expr.charAt(i) == '[')
	        {
	        		openingBracketIndex.add(i);    
	        		openParenBrack.push(expr.charAt(i));
	                location.push(count);
	                count++;
            }
            else if (expr.charAt(i) == ')' || expr.charAt(i) == ']')
            {
                if (openParenBrack.isEmpty())
                        return false;
                else 
                {
                	char ch = openParenBrack.pop();
                    if (expr.charAt(i) == ')' && ch != '(' || expr.charAt(i) == ']' && ch != '[')
                    	return false;
                    closingBracketIndex.set(location.pop(), i);
                }
            }
        }
        if (!openParenBrack.isEmpty())
        	return false;
        return true;
    }

    /**
     * Populates the scalars and arrays lists with symbols for scalar and array
     * variables in the expression. For every variable, a SINGLE symbol is created and stored,
     * even if it appears more than once in the expression.
     * At this time, values for all variables are set to
     * zero - they will be loaded from a file in the loadSymbolValues method.
     */
    public void buildSymbols() 
    {
    	scalars = new ArrayList <ScalarSymbol>();
    	arrays = new ArrayList <ArraySymbol>();
        Stack <String> symbols = new Stack <String>();
        StringTokenizer st = new StringTokenizer(expr, delims, true);       
        String token = "";
        
        while (st.hasMoreTokens())
        {
	        token = st.nextToken();
	        if ((token.charAt(0) >= 'a' && token.charAt(0) <= 'z') || (token.charAt(0) >= 'A' && token.charAt(0) <= 'Z' || token.equals("[")))
	                symbols.push(token);	        
        }
        while(!symbols.isEmpty())
        {
	        token = symbols.pop();
	        if (token.equals("["))
	        {
	            token = symbols.pop();
	            ArraySymbol aSymbol = new ArraySymbol(token);
	            if(arrays.indexOf(aSymbol) == -1)
	            	arrays.add(aSymbol);
	        }
	        else 
	        {
	            ScalarSymbol sSymbol = new ScalarSymbol(token);
	            if (scalars.indexOf(sSymbol) == -1)
	            	scalars.add(sSymbol);
            }
        }
        System.out.println(arrays);
        System.out.println(scalars);
    }
    
    
    /**
     * Loads values for symbols in the expression
     * 
     * @param sc Scanner for values input
     * @throws IOException If there is a problem with the input 
     */
    public void loadSymbolValues(Scanner sc) 
    throws IOException {
        while (sc.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(sc.nextLine().trim());
            int numTokens = st.countTokens();
            String sym = st.nextToken();
            ScalarSymbol ssymbol = new ScalarSymbol(sym);
            ArraySymbol asymbol = new ArraySymbol(sym);
            int ssi = scalars.indexOf(ssymbol);
            int asi = arrays.indexOf(asymbol);
            if (ssi == -1 && asi == -1) {
            	continue;
            }
            int num = Integer.parseInt(st.nextToken());
            if (numTokens == 2) { // scalar symbol
                scalars.get(ssi).value = num;
            } else { // array symbol
            	asymbol = arrays.get(asi);
            	asymbol.values = new int[num];
                // following are (index,val) pairs
                while (st.hasMoreTokens()) {
                    String tok = st.nextToken();
                    StringTokenizer stt = new StringTokenizer(tok," (,)");
                    int index = Integer.parseInt(stt.nextToken());
                    int val = Integer.parseInt(stt.nextToken());
                    asymbol.values[index] = val;              
                }
            }
        }
    }
    
    /**
     * Evaluates the expression, using RECURSION to evaluate subexpressions and to evaluate array 
     * subscript expressions.
     * 
     * @return solution of evaluation
     */
    public float evaluate() 
    {
        return evaluate(expr, expr.length()-1);
    }
    private float evaluate(String expression, int exprEndIndex)
    {
        Stack <String> operators = new Stack <String>();
        Stack <Float> values = new Stack <Float>();
        StringTokenizer st = new StringTokenizer(expression, delims, true);
        String token = "";      
        float constant = 0;
        float scalarValue = 0;
        float bracketValue = 0;
        float parenSolution = 0;
        float arrayLoc = 0;
        String letters = "";
        
        while (st != null) 
        {
        	if (!st.hasMoreTokens())
        		break;
	        token = st.nextToken();
	        if(token.equals("(")||token.equals("["))
	        {
	            int sbi = openingBracketIndex.get((bracketCount));
	            int ebi = this.closingBracketIndex.get(bracketCount);
	            bracketCount++;
	            parenSolution = evaluate(expr.substring(sbi+1, ebi), ebi-1);
                if (token.equals("["))
                {
                	for(int i = 0; i < arrays.size(); i++)
                	{
                		if(arrays.get(i).name.equals(letters))
	                			arrayLoc = i;
                	}
                	int[] arrayvalues = arrays.get((int)arrayLoc).values;
                	bracketValue = arrayvalues[(int)parenSolution];
                	values.push(bracketValue);
                }
                else
                	values.push(parenSolution);
                if (ebi == exprEndIndex)
                  st = null;
                else
                  st = new StringTokenizer(expr.substring(ebi + 1, exprEndIndex + 1), delims, true);
	        }
	        else if ((token.charAt(0) >= 'a' && token.charAt(0) <= 'z'))
	        {
	        	letters = token;    
	        	ScalarSymbol sSymbol = new ScalarSymbol(token);
                int ssi = scalars.indexOf(sSymbol);
                scalarValue = scalars.get(ssi).value;
                values.push(scalarValue);
                checkDivMult(operators, values);
	        }
	        else if((token.charAt(0) >= 'A' && token.charAt(0) <= 'Z'))
	        	letters = token;	       
	        else if (token.equals("+") || token.equals("-") || token.equals("/") || token.equals("*"))
	                operators.push(token);
	        else 
	        {
	        	constant = Integer.parseInt(token);
                values.push(constant);
                checkDivMult(operators, values);
	        }
        }
        if (operators.isEmpty())
        	return values.pop();
        
        Stack <Float> revVal = new Stack <Float>();
        Stack <String> revOp = new Stack <String>();
        while (!operators.isEmpty())
                revOp.push(operators.pop());
        while(!values.isEmpty())
                revVal.push(values.pop());
        while (!revOp.isEmpty())
                processStack(revOp, revVal, false);
        return revVal.pop();
    }
    private void checkDivMult(Stack <String> operators, Stack <Float> values)
    {
        if (!operators.isEmpty())
        {
			String topOp = operators.peek();
			if (topOp.equals("/") || topOp.equals("*"))
				processStack(operators, values, true);
        }
    }
    private void processStack(Stack <String> operators, Stack <Float> values, boolean inOrder)
    {
    	String topOp = operators.pop();
        float temp1 = 0;
        float temp2 = 0;
        float solution = 0;
        if (inOrder)
        {
        	temp2 = values.pop();
            temp1 = values.pop();
        }
        else
        {
            temp1 = values.pop();
            temp2 = values.pop();
        }
        if (topOp.equals("/"))
            solution = temp1 / temp2;       
        else if (topOp.equals("*"))
        	solution = temp1 * temp2;
        else if (topOp.equals("+"))
            solution = temp1 + temp2;
        else if (topOp.equals("-"))  
            solution = temp1 - temp2;
        values.push(solution);
    }
    
    /**
    
     * Utility method, prints the symbols in the scalars list
     */
    public void printScalars() {
        for (ScalarSymbol ss: scalars) {
            System.out.println(ss);
        }
    }
    
    /**
     * Utility method, prints the symbols in the arrays list
     */
    public void printArrays() {
    	for (ArraySymbol as: arrays) {
    		System.out.println(as);
    	}
    }
}