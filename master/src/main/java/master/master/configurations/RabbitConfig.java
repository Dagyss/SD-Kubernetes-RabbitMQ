package master.master.configurations;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Para enviar partes a los workers
    public static final String TASK_QUEUE = "image.parts.queue";
    public static final String TASK_EXCHANGE = "image.exchange";
    public static final String TASK_ROUTING_KEY = "image.part";

    // Para recibir partes procesadas de los workers
    public static final String RESULT_QUEUE = "image.processed.queue";
    public static final String RESULT_ROUTING_KEY = "image.processed";

    @Bean
    public Queue taskQueue() {
        return new Queue(TASK_QUEUE, false);
    }

    @Bean
    public Queue resultQueue() {
        return new Queue(RESULT_QUEUE, false);
    }

    @Bean
    public DirectExchange taskExchange() {
        return new DirectExchange(TASK_EXCHANGE);
    }

    @Bean
    public Binding bindingTaskQueue() {
        return BindingBuilder.bind(taskQueue()).to(taskExchange()).with(TASK_ROUTING_KEY);
    }

    @Bean
    public Binding bindingResultQueue() {
        return BindingBuilder.bind(resultQueue()).to(taskExchange()).with(RESULT_ROUTING_KEY);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }
}
