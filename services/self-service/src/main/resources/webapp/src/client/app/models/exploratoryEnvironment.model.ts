import { ComputationalResourceShapeModel } from "./computationalResourceShape.model";
import { ImageType } from "./imageType.enum";

export class exploratoryEnvironmentName {
  template_name: string;
  description: string;
  environment_type: ImageType;
  version: string;
  vendor: string;
  shapes: Array<ComputationalResourceShapeModel>
}
