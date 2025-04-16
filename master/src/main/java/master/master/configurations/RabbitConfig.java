package master.master.configurations;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String TASK_QUEUE = "image.parts.queue";
    public static final String TASK_EXCHANGE = "image.exchange";
    public static final String TASK_ROUTING_KEY = "image.part";

    @Bean
    public Queue taskQueue() {
        return new Queue(TASK_QUEUE, false);
    }

    @Bean
    public DirectExchange taskExchange() {
        return new DirectExchange(TASK_EXCHANGE);
    }

    @Bean
    public Binding bindingTaskQueue() {
        return BindingBuilder.bind(taskQueue()).to(taskExchange()).with(TASK_ROUTING_KEY);
    }
}

