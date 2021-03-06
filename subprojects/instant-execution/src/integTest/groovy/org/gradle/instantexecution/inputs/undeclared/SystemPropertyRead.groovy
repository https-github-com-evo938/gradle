/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.instantexecution.inputs.undeclared

abstract class SystemPropertyRead {
    String getJavaExpression() {
        return getKotlinExpression()
    }

    String getGroovyExpression() {
        return getKotlinExpression()
    }

    abstract String getKotlinExpression()

    static SystemPropertyRead systemGetProperty(String name) {
        return new SystemPropertyRead() {
            @Override
            String getKotlinExpression() {
                return "System.getProperty(\"$name\")"
            }
        }
    }

    static SystemPropertyRead systemGetPropertyWithDefault(String name, String defaultValue) {
        return new SystemPropertyRead() {
            @Override
            String getKotlinExpression() {
                return "System.getProperty(\"$name\", \"$defaultValue\")"
            }
        }
    }

    static SystemPropertyRead systemGetPropertiesGet(String name) {
        return new SystemPropertyRead() {
            @Override
            String getJavaExpression() {
                return "(String)System.getProperties().get(\"$name\")"
            }

            @Override
            String getGroovyExpression() {
                return "System.properties[\"$name\"]"
            }

            @Override
            String getKotlinExpression() {
                return "System.getProperties()[\"$name\"]"
            }
        }
    }

    static SystemPropertyRead systemGetPropertiesGetProperty(String name) {
        return new SystemPropertyRead() {
            @Override
            String getJavaExpression() {
                return "(String)System.getProperties().getProperty(\"$name\")"
            }

            @Override
            String getGroovyExpression() {
                return "System.properties.getProperty(\"$name\")"
            }

            @Override
            String getKotlinExpression() {
                return "System.getProperties().getProperty(\"$name\")"
            }
        }
    }

    static SystemPropertyRead systemGetPropertiesGetPropertyWithDefault(String name, String defaultValue) {
        return new SystemPropertyRead() {
            @Override
            String getJavaExpression() {
                return "(String)System.getProperties().getProperty(\"$name\", \"$defaultValue\")"
            }

            @Override
            String getGroovyExpression() {
                return "System.properties.getProperty(\"$name\", \"$defaultValue\")"
            }

            @Override
            String getKotlinExpression() {
                return "System.getProperties().getProperty(\"$name\", \"$defaultValue\")"
            }
        }
    }
}
