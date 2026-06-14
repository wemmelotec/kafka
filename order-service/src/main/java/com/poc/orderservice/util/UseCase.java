package com.poc.orderservice.util;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-anotação que marca uma classe como implementação de caso de uso
 * na Arquitetura Hexagonal.
 *
 * <p>{@code @UseCase} carrega {@code @Component} embutido: o Spring enxerga
 * {@code @Component} (por herança de anotação) e registra o bean
 * normalmente, mas o código de {@code application/} nunca importa
 * {@code org.springframework.*} diretamente. Concentrar o único import de
 * Spring do core aqui, em {@code util/} (fora de {@code application/}),
 * significa que {@code CreateOrderService} pode ser testado instanciando-o
 * diretamente com um mock do output port, sem subir contexto Spring.</p>
 *
 * <p>Usa-se {@code @Component} (estereótipo genérico) e não {@code @Service}
 * como anotação embutida — {@code @Service} carrega semântica de "camada de
 * serviço" do MVC, que não existe na Arquitetura Hexagonal.</p>
 *
 * @see org.springframework.stereotype.Component
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface UseCase {
}
