package i5.las2peer.services.apiTestingBot.codex;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.apiTestModel.*;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static i5.las2peer.services.apiTestingBot.codex.CodexTestGen.BASE_PROMPT_CLASS_NAME;
import static i5.las2peer.services.apiTestingBot.codex.CodexTestGen.BASE_PROMPT_METHOD_NAME;

public class CodeToTestModel {

    public class CodeToTestModelException extends Exception {
        public CodeToTestModelException(String message) {
            super(message);
        }
    }

    // try to create test model from code
    public TestRequest convert(String code) throws CodeToTestModelException {
        TryStmt tryStmt = getTryStmtOfTestMethod(code);
        if(tryStmt == null) return null;

        List<ExpressionStmt> statements = tryStmt.getTryBlock().getStatements().stream()
                .filter(statement -> statement instanceof ExpressionStmt)
                .map(ExpressionStmt.class::cast).toList();

        List<VariableDeclarationExpr> variableDeclarationExprs = statements.stream()
                .filter(exp -> exp.getExpression().isVariableDeclarationExpr())
                .map(exp -> exp.getExpression().asVariableDeclarationExpr()).toList();

        String requestMethod = getRequestMethod(variableDeclarationExprs);
        String requestPath = getRequestPath(variableDeclarationExprs);
        JSONObject pathParameters = getPathParams(variableDeclarationExprs, requestPath);
        String body = getBody(variableDeclarationExprs);

        List<MethodCallExpr> methodCallExprs = statements.stream()
                .filter(exp -> exp.getExpression().isMethodCallExpr())
                .map(exp -> exp.getExpression().asMethodCallExpr()).toList();

        List<MethodCallExpr> assertThatExprs = methodCallExprs.stream()
                .filter(exp -> exp.getName().asString().equals("assertThat")).toList();

        List<RequestAssertion> assertions = null;
        try {
            assertions = getAssertions(assertThatExprs);
        } catch (CodeToTestModelException e) {
            JSONObject error = new JSONObject();
            error.put("exception", "CodeToTestModelException");
            error.put("message", e.getMessage());
            String assertionsStr = assertThatExprs.stream().map(exp -> exp.toString()).collect(Collectors.joining(", "));
            error.put("codexGeneratedAssertions", assertionsStr);
            Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_ERROR_1, error.toJSONString());
            throw e;
        }

        return new TestRequest(requestMethod, requestPath, pathParameters, -1, body, assertions);
    }

    private List<RequestAssertion> getAssertions(List<MethodCallExpr> assertThatExprs) throws CodeToTestModelException {
        List<RequestAssertion> assertions = new ArrayList<>();

        for(MethodCallExpr assertThatExp : assertThatExprs) {
            if(assertThatExp.getArguments().size() < 2) continue;

            Expression firstArg = assertThatExp.getArguments().get(0);
            Expression secondArg = assertThatExp.getArguments().get(1);

            if(firstArg.toString().equals("statusCode")) {
                RequestAssertion statusCodeAssertion = getStatusCodeAssertion(secondArg);
                if(statusCodeAssertion != null) assertions.add(statusCodeAssertion);
            } else if(firstArg.toString().equals("response")) {
                RequestAssertion bodyAssertion = getBodyAssertion(secondArg);
                if(bodyAssertion != null) assertions.add(bodyAssertion);
            }
        }
        return assertions;
    }

    private static RequestAssertion getStatusCodeAssertion(Expression arg) {
        if(arg.toString().startsWith("is(")) {
            int statusCode = Integer.valueOf(arg.toString().split("\\(")[1].split("\\)")[0]);
            return new StatusCodeAssertion(0, statusCode);
        }
        return null;
    }

    private RequestAssertion getBodyAssertion(Expression arg) throws CodeToTestModelException {
        Expression current = arg;
        BodyAssertionOperator operator = null;
        while(current != null) {
            if(!current.isMethodCallExpr()) break;
            List<Node> childNodes = current.getChildNodes();

            BodyAssertionOperator followingOperator = null;

            String methodName = current.asMethodCallExpr().getName().asString();
            if(methodName.equals("isA")) {
                // create "has type" assertion
                Expression e = ((Expression) childNodes.get(1));
                if(e.isClassExpr()) {
                    String className = e.asClassExpr().getType().asString();
                    int inputType = classNameToInputType(className);
                    followingOperator = new BodyAssertionOperator(0, inputType);
                }
                current = null;
            } else if(methodName.equals("asJSONObject") || methodName.equals("asJSONObjectList")) {
                // continue with first argument of asJSONObject or asJSONObjectList
                if(childNodes.size() > 1) current = (Expression) childNodes.get(1);
                else current = null;
            } else if(methodName.equals("everyItem")) {
                if(childNodes.size() == 2) {
                    followingOperator = new BodyAssertionOperator(3, 0);
                    current = (Expression) childNodes.get(1);
                } else {
                    throw new CodeToTestModelException("Error: everyItem has more/less than 1 argument");
                }
            } else if(methodName.equals("hasField")) {
                Node hasFieldFirstArg = childNodes.get(1);

                if(hasFieldFirstArg instanceof StringLiteralExpr) {
                    followingOperator = new BodyAssertionOperator(1, 1, ((StringLiteralExpr) hasFieldFirstArg).getValue(), null);
                    // check if there is a second argument
                    if(childNodes.size() > 2) {
                        Node hasFieldSecondArg = childNodes.get(2);
                        current = (Expression) hasFieldSecondArg;
                    } else {
                        current = null;
                    }
                } else {
                    throw new CodeToTestModelException("Error, hasField first arg is not StringLiteralExpr");
                }
            } else {
                throw new CodeToTestModelException("methodName " + methodName + " unsupported here!");
            }

            if(operator == null) {
                operator = followingOperator;
            } else {
                BodyAssertionOperator last = operator;
                while(last.hasFollowingOperator()) last = last.getFollowingOperator();
                last.setFollowedByOperator(followingOperator);
            }
        }

        if(operator != null) return new BodyAssertion(operator);
        return null;
    }

    private static int classNameToInputType(String className) {
        switch (className) {
            case "JSONObject":
                return 2;
            case "JSONArray":
                return 3;
            case "String":
                return 4;
            case "Number":
                return 5;
            case "Boolean":
                return 6;
            default:
                return -1;
        }
    }

    private static String getRequestMethod(List<VariableDeclarationExpr> variableDeclarationExprs) {
        List<Node> nodes = getSendRequestArgs(variableDeclarationExprs);
        return ((StringLiteralExpr) nodes.get(2)).getValue();
    }

    private static String getRequestPath(List<VariableDeclarationExpr> variableDeclarationExprs) {
        List<Node> nodes = getSendRequestArgs(variableDeclarationExprs);
        return ((BinaryExpr) nodes.get(3)).getRight().asStringLiteralExpr().getValue();
    }

    private static JSONObject getPathParams(List<VariableDeclarationExpr> variableDeclarationExprs, String requestPath) {
        List<Node> nodes = getSendRequestArgs(variableDeclarationExprs);
        JSONObject pathParameters = new JSONObject();

        Pattern p = Pattern.compile("\\{([^}]*)}");
        int i = 7;
        for(Object match : p.matcher(requestPath).results().toArray()) {
            MatchResult result = (MatchResult) match;
            String paramName = requestPath.substring(result.start() + 1, result.end() - 1);
            if (i < nodes.size()) {
                Node node = nodes.get(i);
                String paramValue = "?";
                if(node instanceof StringLiteralExpr) {
                    paramValue = ((StringLiteralExpr) nodes.get(i)).getValue();
                } else if(node instanceof IntegerLiteralExpr) {
                    paramValue = ((IntegerLiteralExpr) nodes.get(i)).getValue();
                }
                pathParameters.put(paramName, paramValue);
                i++;
            } else {
                break;
            }
        }
        return pathParameters;
    }

    private static String getBody(List<VariableDeclarationExpr> variableDeclarationExprs) {
        List<Node> nodes = getSendRequestArgs(variableDeclarationExprs);
        String body = "";
        Expression bodyExp = (Expression) nodes.get(4);
        if(bodyExp instanceof NullLiteralExpr) {
            // body is null
        } else if(bodyExp instanceof StringLiteralExpr) {
            body = ((StringLiteralExpr) bodyExp).getValue();
        } else if(bodyExp instanceof TextBlockLiteralExpr) {
            body = ((TextBlockLiteralExpr) bodyExp).getValue();
        }
        return body;
    }

    private static List<Node> getSendRequestArgs(List<VariableDeclarationExpr> variableDeclarationExprs) {
        for(VariableDeclarationExpr varDeclExp : variableDeclarationExprs) {
            if (varDeclExp.getElementType().toString().equals("ClientResponse")) {
                VariableDeclarator d = varDeclExp.getVariables().get(0);
                Expression e = d.getInitializer().get();
                return e.getChildNodes();
            }
        }
        return null;
    }

    private static TryStmt getTryStmtOfTestMethod(String code) {
        CompilationUnit unit = new JavaParser().parse(code).getResult().get();
        MethodDeclaration methodDeclaration = unit.getClassByName(BASE_PROMPT_CLASS_NAME).get()
                .getMethodsByName(BASE_PROMPT_METHOD_NAME).get(0);
        for(Statement statement : methodDeclaration.getBody().get().getStatements()) {
            if(statement instanceof TryStmt) return (TryStmt) statement;
        }
        return null;
    }
}
