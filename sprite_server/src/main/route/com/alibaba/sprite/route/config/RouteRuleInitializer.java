/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * (created at 2012-6-15)
 */
package com.alibaba.sprite.route.config;

import java.sql.SQLSyntaxErrorException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.sprite.config.loader.SchemaLoader;
import com.alibaba.sprite.config.model.rule.RuleAlgorithm;
import com.alibaba.sprite.config.model.rule.RuleConfig;
import com.alibaba.sprite.config.util.ConfigException;
import com.alibaba.sprite.parser.ast.expression.Expression;
import com.alibaba.sprite.parser.ast.expression.primary.function.FunctionExpression;
import com.alibaba.sprite.parser.recognizer.mysql.MySQLFunctionManager;
import com.alibaba.sprite.parser.recognizer.mysql.MySQLToken;
import com.alibaba.sprite.parser.recognizer.mysql.lexer.MySQLLexer;
import com.alibaba.sprite.parser.recognizer.mysql.syntax.MySQLExprParser;
import com.alibaba.sprite.parser.recognizer.mysql.syntax.MySQLParser;
import com.alibaba.sprite.route.function.ExpressionAdapter;

/**
 * @author <a href="mailto:shuo.qius@alibaba-inc.com">QIU Shuo</a>
 */
public class RouteRuleInitializer {
    public static void initRouteRule(SchemaLoader loader) throws SQLSyntaxErrorException {
        Map<String, RuleAlgorithm> functions = loader.getFunctions();
        MySQLFunctionManager functionManager = new MySQLFunctionManager(true);
        buildFuncManager(functionManager, functions);
        for (RuleConfig conf : loader.listRuleConfig()) {
            String algorithmString = conf.getAlgorithm();
            MySQLLexer lexer = new MySQLLexer(algorithmString);
            MySQLExprParser parser = new MySQLExprParser(lexer, functionManager, false, MySQLParser.DEFAULT_CHARSET);
            Expression expression = parser.expression();
            if (lexer.token() != MySQLToken.EOF) {
                throw new ConfigException("route algorithm not end with EOF: " + algorithmString);
            }
            RuleAlgorithm algorithm;
            if (expression instanceof RuleAlgorithm) {
                algorithm = (RuleAlgorithm) expression;
            } else {
                algorithm = new ExpressionAdapter(expression);
            }
            conf.setRuleAlgorithm(algorithm);
        }
    }

    private static void buildFuncManager(MySQLFunctionManager functionManager, Map<String, RuleAlgorithm> functions) {
        Map<String, FunctionExpression> extFuncPrototypeMap = new HashMap<String, FunctionExpression>(functions.size());
        for (Entry<String, RuleAlgorithm> en : functions.entrySet()) {
            extFuncPrototypeMap.put(en.getKey(), (FunctionExpression) en.getValue());
        }
        functionManager.addExtendFunction(extFuncPrototypeMap);
    }
}
