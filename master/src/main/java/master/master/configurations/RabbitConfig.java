package master.master.configurations;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue taskQueue() {
        return new Queue("sobel-tasks", true);
    }

    @Bean
    public Queue resultQueue() {
        return new Queue("sobel-results", true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("sobel-exchange");
    }

    @Bean
    public Binding taskBinding() {
        return BindingBuilder.bind(taskQueue()).to(exchange()).with("task.routing.key");
    }

    @Bean
    public Binding resultBinding() {
        return BindingBuilder.bind(resultQueue()).to(exchange()).with("result.routing.key");
    }
}
