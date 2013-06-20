package org.wildfly.clustering.singleton;

import org.wildfly.clustering.dispatcher.Command;

public interface SingletonCommand<R, T> extends Command<R, SingletonContext<T>> {

}
