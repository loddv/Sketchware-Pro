package pro.sketchware.blocks.generator.handlers;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.besome.sketch.beans.BlockBean;
import pro.sketchware.blocks.generator.builders.BinaryExprOperatorsTreeBuilder;
import pro.sketchware.blocks.generator.EventBlocksGenerator;
import pro.sketchware.blocks.generator.interfaces.StatementHandler;
import pro.sketchware.blocks.generator.records.HandlerContext;
import pro.sketchware.blocks.generator.records.RequiredBlockType;

import java.util.List;

public class WhileStatementHandler implements StatementHandler {

    private final BinaryExprOperatorsTreeBuilder binaryExprOperatorsTreeBuilder;
    private final EventBlocksGenerator parent;

    public WhileStatementHandler(BinaryExprOperatorsTreeBuilder binaryExprOperatorsTreeBuilder, EventBlocksGenerator parent) {
        this.binaryExprOperatorsTreeBuilder = binaryExprOperatorsTreeBuilder;
        this.parent = parent;
    }

    @Override
    public boolean canHandle(Statement stmt) {
        return stmt instanceof WhileStmt;
    }

    @Override
    public void handle(Statement stmt, HandlerContext context) {
        WhileStmt ws = (WhileStmt) stmt;
        Expression expr = ws.getCondition();
        List<BlockBean> blockBeans = context.blockBeans();
        List<String> noNextBlocks = context.noNextBlocks();
        boolean isForeverBlock = false;
        if (expr.isBooleanLiteralExpr()) {
            isForeverBlock = expr.toString().equals("true");
        }
        BlockBean wb = new BlockBean(String.valueOf(context.idCounter().getAndIncrement()),
                isForeverBlock ? "forever" : "while %b",
                "c",
                "",
                isForeverBlock ? "forever" : "whileLoop"
        );

        if (!isForeverBlock) {
            List<BlockBean> condTree = binaryExprOperatorsTreeBuilder.build(ws.getCondition(), new RequiredBlockType("b"));
            blockBeans.addAll(condTree);
            BlockBean condRoot = condTree.get(condTree.size() - 1);
            condRoot.nextBlock = -1;
            wb.parameters.add("@" + condRoot.id);
        }
        blockBeans.add(wb);
        if (ws.getBody().isBlockStmt()) {
            var body = ws.getBody().asBlockStmt().getStatements();
            if (body.isEmpty()) {
                wb.subStack1 = -1;
            } else {
                wb.subStack1 = context.idCounter().get();
                for (int i = 0; i < body.size(); i++) {
                    if (body.size() == 1 || i == body.size() - 1) {
                        noNextBlocks.add(String.valueOf(context.idCounter().get()));
                    }
                    Statement s = body.get(i);
                    parent.processStatement(s);
                }
            }
        } else {
            wb.subStack1 = -1;
        }
        wb.nextBlock = context.idCounter().get();
    }

}
