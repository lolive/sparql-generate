/*
 * Copyright 2016 The Apache Software Foundation.
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
package com.github.thesmartenergy.sparql.generate.jena.engine.impl;

import com.github.thesmartenergy.sparql.generate.jena.engine.IteratorPlan;
import java.util.List;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import com.github.thesmartenergy.sparql.generate.jena.iterator.IteratorFunction;
import java.util.ArrayList;
import java.util.function.Consumer;
import org.apache.jena.sparql.expr.VariableNotBoundException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Executes a {@code ITERATOR <iterator>(<expreList>) AS <var>} clause.
 *
 * @author Maxime Lefrançois <maxime.lefrancois at emse.fr>
 */
public class IteratorPlanImpl extends PlanBase implements IteratorPlan {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(IteratorPlanImpl.class);


    /**
     * The SPARQL-Generate iterator.
     */
    private final IteratorFunction iterator;

    /**
     * The list of expressions on which to evaluate the iterator.
     */
    private final ExprList exprList;

    /**
     * The variable that will be bound to each result of the iterator
     * evaluation.
     */
    private final Var var;

    /**
     * The constructor.
     *
     * @param s - The SPARQL-Generate iterator function.
     * @param e - The list of expressions on which to evaluate the
     * iterator function.
     * @param v - The variable that will be bound to each result of the iterator
     *  function evaluation.
     */
    public IteratorPlanImpl(
            final IteratorFunction s,
            final ExprList e,
            final Var v) {
        this.iterator = s;
        this.exprList = e;
        this.var = v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    final public void exec(
            final List<Var> variables,
            final List<BindingHashMapOverwrite> values,
            final Consumer<List<BindingHashMapOverwrite>> bindingStream ) {

        boolean added = variables.add(var);
        if (!added) {
            LOG.debug("Bindings of variable " + var + " will be overriden");
        }

        ensureNotEmpty(variables, values);
        final StringBuilder sb;
        if(LOG.isTraceEnabled()) {
            sb = new StringBuilder("Execution of " + iterator + " " + exprList + " AS  " + var + ":\n");
        } else {
            sb = null;
        }
        values.forEach((binding) -> {
            List<BindingHashMapOverwrite> newValues= new ArrayList();
            try {
                iterator.exec(binding, exprList, null, (nodeValues) -> {
                    if (nodeValues == null || nodeValues.isEmpty()) {
                        newValues.add(new BindingHashMapOverwrite(binding, var, null));
                    } else {
                        nodeValues.forEach((nodeValue) -> {
                            newValues.add(
                                    new BindingHashMapOverwrite(
                                            binding, var, nodeValue.asNode()));
                        });
                        bindingStream.accept(newValues);
                    }
                });
        if(LOG.isTraceEnabled()) {
            sb.setLength(sb.length() - 2);
        }
        LOG.trace(sb.toString());
            } catch (ExprEvalException ex) {
                LOG.debug("Iterator execution failed: ", ex);
                newValues.add(new BindingHashMapOverwrite(binding, var, null));
            }
        });
    }
}