package no.nav.kanal.camel

import org.apache.camel.Message
import org.apache.camel.Predicate
import org.apache.camel.builder.ExpressionClause
import org.apache.camel.model.ChoiceDefinition
import org.apache.camel.model.ProcessorDefinition

inline fun <reified T: Any> Message.header(name: String): T = getHeader(name, T::class.java)
inline fun <reified T: Any> Message.body(): T = getBody(T::class.java)
inline fun <reified T: ProcessorDefinition<*>> ProcessorDefinition<*>.choice(choice: ChoiceDefinition.() -> T): ProcessorDefinition<*> = choice(this.choice()).endChoice()
inline fun ChoiceDefinition.doWhen(predicate: Predicate, expr: ChoiceDefinition.() -> ProcessorDefinition<*>) = `when`(predicate).apply { expr(this) }
inline fun ChoiceDefinition.otherwise(expr: ChoiceDefinition.() -> ProcessorDefinition<*>) = expr()
