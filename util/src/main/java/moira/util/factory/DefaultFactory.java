package moira.util.factory;

import moira.util.execution.ForkExecutor;
import moira.util.service.DefaultService;
import moira.util.service.Service;

public class DefaultFactory implements MoiraFactory {
  @Override
  public Service createService() {
    return new DefaultService(new ForkExecutor());
  }
}
