/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2022, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.joran;

import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.action.AppenderAction;
import ch.qos.logback.core.joran.action.AppenderRefAction;
import ch.qos.logback.core.joran.action.ContextPropertyAction;
import ch.qos.logback.core.joran.action.ConversionRuleAction;
import ch.qos.logback.core.joran.action.DefinePropertyAction;
import ch.qos.logback.core.joran.action.EventEvaluatorAction;
import ch.qos.logback.core.joran.action.ImplicitModelAction;
import ch.qos.logback.core.joran.action.ImportAction;
import ch.qos.logback.core.joran.action.NewRuleAction;
import ch.qos.logback.core.joran.action.ParamAction;
import ch.qos.logback.core.joran.action.PropertyAction;
import ch.qos.logback.core.joran.action.ShutdownHookAction;
import ch.qos.logback.core.joran.action.StatusListenerAction;
import ch.qos.logback.core.joran.action.TimestampAction;
import ch.qos.logback.core.joran.conditional.ElseAction;
import ch.qos.logback.core.joran.conditional.IfAction;
import ch.qos.logback.core.joran.conditional.ThenAction;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.joran.spi.SaxEventInterpreter;
import ch.qos.logback.core.model.DefineModel;
import ch.qos.logback.core.model.EventEvaluatorModel;
import ch.qos.logback.core.model.ImplicitModel;
import ch.qos.logback.core.model.ImportModel;
import ch.qos.logback.core.model.IncludeModel;
import ch.qos.logback.core.model.ParamModel;
import ch.qos.logback.core.model.PropertyModel;
import ch.qos.logback.core.model.ShutdownHookModel;
import ch.qos.logback.core.model.StatusListenerModel;
import ch.qos.logback.core.model.TimestampModel;
import ch.qos.logback.core.model.conditional.ElseModel;
import ch.qos.logback.core.model.conditional.IfModel;
import ch.qos.logback.core.model.conditional.ThenModel;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.model.processor.DefineModelHandler;
import ch.qos.logback.core.model.processor.EventEvaluatorModelHandler;
import ch.qos.logback.core.model.processor.ImplicitModelHandler;
import ch.qos.logback.core.model.processor.ImportModelHandler;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.model.processor.NOPModelHandler;
import ch.qos.logback.core.model.processor.PropertyModelHandler;
import ch.qos.logback.core.model.processor.ShutdownHookModelHandler;
import ch.qos.logback.core.model.processor.StatusListenerModelHandler;
import ch.qos.logback.core.model.processor.TimestampModelHandler;
import ch.qos.logback.core.model.processor.conditional.ElseModelHandler;
import ch.qos.logback.core.model.processor.conditional.IfModelHandler;
import ch.qos.logback.core.model.processor.conditional.ThenModelHandler;
import ch.qos.logback.core.spi.AppenderAttachable;

// Based on 310985 revision 310985 as attested by http://tinyurl.com/8njps
// see also http://tinyurl.com/c2rp5

/**
 * A JoranConfiguratorBase lays most of the groundwork for concrete
 * configurators derived from it. Concrete configurators only need to implement
 * the {@link #addInstanceRules} method.
 * <p>
 * A JoranConfiguratorBase instance should not be used more than once to
 * configure a Context.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
abstract public class JoranConfiguratorBase<E> extends GenericXMLConfigurator {

    @Override
    protected void addInstanceRules(RuleStore rs) {

        // is "configuration/variable" referenced in the docs?
        rs.addRule(new ElementSelector("configuration/variable"), () -> new PropertyAction());
        rs.addRule(new ElementSelector("configuration/import"), () -> new ImportAction());
        rs.addRule(new ElementSelector("configuration/property"), () -> new PropertyAction());

        rs.addRule(new ElementSelector("configuration/substitutionProperty"), () -> new PropertyAction());

        rs.addRule(new ElementSelector("configuration/timestamp"), () -> new TimestampAction());
        rs.addRule(new ElementSelector("configuration/shutdownHook"), () -> new ShutdownHookAction());
        rs.addRule(new ElementSelector("configuration/define"), () -> new DefinePropertyAction());
        rs.addRule(new ElementSelector("configuration/evaluator"), () -> new EventEvaluatorAction());

        // the contextProperty pattern is deprecated. It is undocumented
        // and will be dropped in future versions of logback
        rs.addRule(new ElementSelector("configuration/contextProperty"), () -> new ContextPropertyAction());

        rs.addRule(new ElementSelector("configuration/conversionRule"), () -> new ConversionRuleAction());

        rs.addRule(new ElementSelector("configuration/statusListener"), () -> new StatusListenerAction());

        rs.addRule(new ElementSelector("configuration/appender"), () -> new AppenderAction());
        rs.addRule(new ElementSelector("configuration/appender/appender-ref"), () -> new AppenderRefAction());
        rs.addRule(new ElementSelector("configuration/newRule"), () -> new NewRuleAction());

        rs.addRule(new ElementSelector("*/param"), () -> new ParamAction());

        // add if-then-else support
        rs.addRule(new ElementSelector("*/if"), () -> new IfAction());
        rs.addTransparentPathPart("if");
        rs.addRule(new ElementSelector("*/if/then"), () -> new ThenAction());
        rs.addTransparentPathPart("then");
        rs.addRule(new ElementSelector("*/if/else"), () -> new ElseAction());
        rs.addTransparentPathPart("else");
    }

    @Override
    protected void setImplicitRuleSupplier(SaxEventInterpreter interpreter) {
        interpreter.setImplicitActionSupplier( () -> new ImplicitModelAction() );
    }

    public void buildModelInterprtationContext() {
        super.buildModelInterprtationContext();
        Map<String, Object> omap = modelInterpretationContext.getObjectMap();
        omap.put(JoranConstants.APPENDER_BAG, new HashMap<String, Appender<?>>());
        omap.put(JoranConstants.APPENDER_REF_BAG, new HashMap<String, AppenderAttachable<?>>());
    }

    public SaxEventInterpretationContext getInterpretationContext() {
        return saxEventInterpreter.getSaxEventInterpretationContext();
    }

    @Override
    protected DefaultProcessor buildDefaultProcessor(Context context, ModelInterpretationContext mic) {
        DefaultProcessor defaultProcessor = super.buildDefaultProcessor(context, mic);
        defaultProcessor.addHandler(ImportModel.class, ImportModelHandler::makeInstance);

        defaultProcessor.addHandler(ShutdownHookModel.class, ShutdownHookModelHandler::makeInstance);
        defaultProcessor.addHandler(EventEvaluatorModel.class, EventEvaluatorModelHandler::makeInstance);
        defaultProcessor.addHandler(DefineModel.class, DefineModelHandler::makeInstance);
        defaultProcessor.addHandler(IncludeModel.class, NOPModelHandler::makeInstance);

        
        defaultProcessor.addHandler(ParamModel.class, ParamModelHandler::makeInstance);
        defaultProcessor.addHandler(PropertyModel.class, PropertyModelHandler::makeInstance);
        defaultProcessor.addHandler(TimestampModel.class, TimestampModelHandler::makeInstance);
        defaultProcessor.addHandler(StatusListenerModel.class, StatusListenerModelHandler::makeInstance);
        defaultProcessor.addHandler(ImplicitModel.class, ImplicitModelHandler::makeInstance);
        
        defaultProcessor.addHandler(IfModel.class, IfModelHandler::makeInstance);
        defaultProcessor.addHandler(ThenModel.class, ThenModelHandler::makeInstance);
        defaultProcessor.addHandler(ElseModel.class, ElseModelHandler::makeInstance);

        return defaultProcessor;
    }

}
