package gov.ca.emsa.pulse.broker;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@PropertySource("classpath:/environment.test.properties")
@EnableTransactionManagement
@SpringBootApplication(scanBasePackages= {"gov.ca.emsa.pulse.broker.manager.**",
		"gov.ca.emsa.pulse.broker.**",
		"gov.ca.emsa.pulse.broker.dao.**",
		"gov.ca.emsa.pulse.broker.entity.**"})
public class BrokerApplicationTestConfig implements EnvironmentAware {
	
	private Environment env;
	
	@Override
	public void setEnvironment(final Environment e) {
		this.env = e;
	}
	
	@Bean
	public DataSource dataSource() {
      PGSimpleDataSource ds = new PGSimpleDataSource();
  	ds.setServerName(env.getRequiredProperty("testDbServer"));
      ds.setUser(env.getRequiredProperty("testDbUser"));
      ds.setPassword(env.getRequiredProperty("testDbPassword"));
		return ds;
	}
	
	@Bean
	public org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean entityManagerFactory(){
		org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean bean = new org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean();
		bean.setDataSource(dataSource());
		bean.setPersistenceUnitName(env.getProperty("persistenceUnitName"));
		return bean;
	}
	
	@Bean
	public org.springframework.orm.jpa.JpaTransactionManager transactionManager(){
		org.springframework.orm.jpa.JpaTransactionManager bean = new org.springframework.orm.jpa.JpaTransactionManager();
		bean.setEntityManagerFactory(entityManagerFactory().getObject());
		return bean;
	}
	
	@Bean
	public org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor persistenceAnnotationBeanPostProcessor(){
		return new org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor();
	}
	
	
	@Bean
	public MappingJackson2HttpMessageConverter jsonConverter(){
		
		MappingJackson2HttpMessageConverter bean = new MappingJackson2HttpMessageConverter();
		
		bean.setPrefixJson(false);
		
		List<MediaType> mediaTypes = new ArrayList<MediaType>();
		mediaTypes.add(MediaType.APPLICATION_JSON);
		
		bean.setSupportedMediaTypes(mediaTypes);
		
		return bean;
	}
	
}